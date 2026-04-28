package com.gxa.pipe.virtualExpert.agent;

import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VirtualExpertExportServiceTest {

    private final VirtualExpertExportService service = new VirtualExpertExportService(new FakeAgentClient());

    @Test
    void createPdfExportWritesDownloadablePdf() throws Exception {
        ExportFile file = service.createExport(Map.of("format", "pdf", "sessionId", "ana_001"));

        assertThat(file.fileName()).endsWith(".pdf");
        assertThat(file.contentType()).isEqualTo("application/pdf");
        byte[] header = Files.readAllBytes(file.path());
        assertThat(new String(header, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void createExcelExportWritesXlsxZip() throws Exception {
        ExportFile file = service.createExport(Map.of("format", "excel", "sessionId", "ana_001"));
        byte[] header = Files.readAllBytes(file.path());

        assertThat(file.fileName()).endsWith(".xlsx");
        assertThat(file.contentType()).contains("spreadsheetml");
        assertThat(header[0]).isEqualTo((byte) 'P');
        assertThat(header[1]).isEqualTo((byte) 'K');
    }

    @Test
    void createPdfExportFallsBackToSessionSummaryWhenTimelineIsMissing() throws Exception {
        VirtualExpertExportService fallbackService = new VirtualExpertExportService(new EmptyTimelineAgentClient());

        ExportFile file = fallbackService.createExport(Map.of("format", "pdf", "sessionId", "ana_empty"));

        assertThat(file.fileName()).endsWith(".pdf");
        assertThat(Files.exists(file.path())).isTrue();
    }

    @Test
    void createExportRejectsUnsupportedPlanFormat() {
        assertThatThrownBy(() -> service.createExport(Map.of(
                "sessionId", "ana_001",
                "exportPlan", Map.of("format", "docx")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported export format");
    }

    @Test
    void createExcelExportUsesSanitizedPlanTitleAndSheets() throws Exception {
        ExportFile file = service.createExport(Map.of(
                "sessionId", "ana_001",
                "exportPlan", Map.of(
                        "format", "excel",
                        "title", "A/B:C*D?E",
                        "tables", List.of(
                                Map.of("type", "risk_list", "title", "风险/清单"),
                                Map.of("type", "action_list", "title", "处置*建议")
                        )
                )
        ));

        assertThat(file.fileName()).endsWith(".xlsx");
        assertThat(Files.exists(file.path())).isTrue();
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(file.path()))) {
            assertThat(workbook.getSheet("风险-清单")).isNotNull();
            assertThat(workbook.getSheet("处置-建议")).isNotNull();
        }
    }

    @Test
    void createExcelExportIncludesEveryTurnForFullSessionScope() throws Exception {
        VirtualExpertExportService fullSessionService = new VirtualExpertExportService(new PagedTimelineAgentClient());

        ExportFile file = fullSessionService.createExport(Map.of(
                "sessionId", "ana_001",
                "format", "excel",
                "exportPlan", Map.of("scope", "full_session", "format", "excel")
        ));

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(file.path()))) {
            assertThat(workbook.getSheet("轮次明细")).isNotNull();
            assertThat(workbook.getSheet("轮次明细").getPhysicalNumberOfRows()).isGreaterThanOrEqualTo(3);
            assertThat(workbook.getSheet("轮次明细").getRow(1).getCell(2).getStringCellValue()).contains("第一轮");
            assertThat(workbook.getSheet("轮次明细").getRow(2).getCell(2).getStringCellValue()).contains("第二轮");
        }
    }

    @Test
    void createExcelExportIncludesMultipleSessionsForAllConversationsScope() throws Exception {
        VirtualExpertExportService allConversationsService = new VirtualExpertExportService(new AllConversationsAgentClient());

        ExportFile file = allConversationsService.createExport(Map.of(
                "sessionId", "ana_001",
                "format", "excel",
                "exportPlan", Map.of("scope", "all_conversations", "format", "excel")
        ));

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(file.path()))) {
            assertThat(workbook.getSheet("会话汇总")).isNotNull();
            assertThat(workbook.getSheet("会话汇总").getPhysicalNumberOfRows()).isGreaterThanOrEqualTo(3);
            assertThat(workbook.getSheet("会话汇总").getRow(1).getCell(0).getStringCellValue()).isEqualTo("ana_001");
            assertThat(workbook.getSheet("会话汇总").getRow(2).getCell(0).getStringCellValue()).isEqualTo("ana_002");
        }
    }

    private static class FakeAgentClient extends AgentClient {
        FakeAgentClient() {
            super(
                    new AgentServiceProperties("http://agent.local", "dev-agent-secret", "pipeline-agent"),
                    new RestTemplateBuilder().build()
            );
        }

        @Override
        public Map<String, Object> listTimeline(String sessionId, String beforeCursor, Integer limit) {
            return Map.of(
                    "sessionId", sessionId,
                    "items", List.of(Map.of(
                            "userMessage", Map.of(
                                    "content", "CP-04 压力异常，请判断是否需要停输",
                                    "createdAt", "2026-04-27T00:00:00Z"
                            ),
                            "run", Map.of(
                                    "id", "run_001",
                                    "summary", Map.of(
                                            "riskLevel", "high",
                                            "judgment", "压力异常需要现场复核，暂不建议立即停输。",
                                            "recommendedActions", List.of("复核传感器", "检查上游阀室"),
                                            "openQuestions", List.of("缺少近 24 小时压力趋势")
                                    )
                            )
                    ))
            );
        }

        @Override
        public Map<String, Object> listRunEvents(String sessionId, String runId, Long afterSeq) {
            return Map.of(
                    "sessionId", sessionId,
                    "runId", runId,
                    "events", List.of(
                            Map.of("type", "tool_completed", "message", "查询到压力趋势"),
                            Map.of("type", "recommendation_generated", "payload", Map.of("summary", "建议现场复核"))
                    )
            );
        }
    }

    private static class EmptyTimelineAgentClient extends FakeAgentClient {
        @Override
        public Map<String, Object> listTimeline(String sessionId, String beforeCursor, Integer limit) {
            return Map.of("sessionId", sessionId, "items", List.of());
        }

        @Override
        public Map<String, Object> listSessions(Integer limit) {
            return Map.of("sessions", List.of(Map.of(
                    "id", "ana_empty",
                    "title", "旧会话",
                    "summary", "历史摘要"
            )));
        }
    }

    private static class PagedTimelineAgentClient extends FakeAgentClient {
        @Override
        public Map<String, Object> listTimeline(String sessionId, String beforeCursor, Integer limit) {
            if (beforeCursor == null || beforeCursor.isBlank()) {
                return Map.of(
                        "sessionId", sessionId,
                        "items", List.of(timelineItem("msg_002", "cursor_002", "第二轮：阀门已复核，继续判断传感器", "run_002")),
                        "hasMoreBefore", true,
                        "beforeCursor", "cursor_002"
                );
            }
            return Map.of(
                    "sessionId", sessionId,
                    "items", List.of(timelineItem("msg_001", "cursor_001", "第一轮：CP-04 压力异常", "run_001")),
                    "hasMoreBefore", false,
                    "beforeCursor", "cursor_001"
            );
        }
    }

    private static class AllConversationsAgentClient extends PagedTimelineAgentClient {
        @Override
        public Map<String, Object> listSessions(Integer limit) {
            return Map.of("sessions", List.of(
                    Map.of("id", "ana_001", "title", "CP-04 会话", "status", "completed", "summary", "压力异常"),
                    Map.of("id", "ana_002", "title", "东区泵站会话", "status", "completed", "summary", "流量突降")
            ));
        }

        @Override
        public Map<String, Object> listTimeline(String sessionId, String beforeCursor, Integer limit) {
            return Map.of(
                    "sessionId", sessionId,
                    "items", List.of(timelineItem("msg_" + sessionId, "cursor_" + sessionId, "问题：" + sessionId, "run_" + sessionId)),
                    "hasMoreBefore", false,
                    "beforeCursor", "cursor_" + sessionId
            );
        }
    }

    private static Map<String, Object> timelineItem(String messageId, String cursor, String content, String runId) {
        return Map.of(
                "turnId", messageId,
                "cursor", cursor,
                "userMessage", Map.of(
                        "id", messageId,
                        "content", content,
                        "createdAt", "2026-04-27T00:00:00Z"
                ),
                "run", Map.of(
                        "id", runId,
                        "status", "completed",
                        "summary", Map.of(
                                "riskLevel", "medium",
                                "judgment", "建议复核现场状态",
                                "recommendedActions", List.of("复核传感器")
                        ),
                        "eventCount", 1
                )
        );
    }
}
