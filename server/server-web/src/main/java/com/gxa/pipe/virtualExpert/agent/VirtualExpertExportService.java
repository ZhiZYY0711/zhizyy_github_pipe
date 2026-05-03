package com.gxa.pipe.virtualExpert.agent;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class VirtualExpertExportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PDF = "pdf";
    private static final String EXCEL = "excel";
    private static final String TXT = "txt";
    private static final String MARKDOWN = "md";
    private static final String DOCX = "docx";

    private final AgentClient agentClient;
    private final Map<String, ExportFile> files = new ConcurrentHashMap<>();

    public ExportFile createExport(Map<String, Object> request) {
        String sessionId = text(request.get("sessionId"));
        if (sessionId.isBlank()) {
            sessionId = text(request.get("session_id"));
        }
        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        Map<String, Object> exportPlan = map(firstNonNull(request.get("exportPlan"), request.get("export_plan")));
        String format = normalizeFormat(firstNonBlank(text(exportPlan.get("format")), text(request.getOrDefault("format", PDF))));
        ExportSnapshot snapshot = loadSnapshot(sessionId, text(request.get("runId")), exportPlan);
        try {
            return switch (format) {
                case PDF -> writePdf(snapshot);
                case EXCEL -> writeExcel(snapshot);
                case TXT -> writeText(snapshot);
                case MARKDOWN -> writeMarkdown(snapshot);
                case DOCX -> writeDocx(snapshot);
                default -> throw new IllegalArgumentException("Unsupported export format: " + format);
            };
        } catch (IOException e) {
            throw new IllegalStateException("Export file generation failed", e);
        }
    }

    public Optional<ExportFile> findExport(String exportId) {
        return Optional.ofNullable(files.get(exportId)).filter(file -> Files.exists(file.path()));
    }

    public List<ExportFile> listExports(String format, int limit) {
        String normalizedFormat = text(format).isBlank() ? "" : normalizeFormat(format);
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        return files.values().stream()
                .filter(file -> Files.exists(file.path()))
                .filter(file -> normalizedFormat.isBlank() || contentTypeMatches(file.contentType(), normalizedFormat))
                .limit(boundedLimit)
                .toList();
    }

    private ExportSnapshot loadSnapshot(String sessionId, String requestedRunId, Map<String, Object> exportPlan) {
        String scope = text(exportPlan.getOrDefault("scope", "agent_selected"));
        int maxSessions = intValue(exportPlan.get("maxSessions"), 30, 1, 100);
        int maxTurns = intValue(exportPlan.get("maxTurnsPerSession"), 20, 1, 50);
        boolean includeEvidence = booleanValue(exportPlan.get("includeEvidence"), true);
        boolean includeTimeline = booleanValue(exportPlan.get("includeTimeline"), true);

        List<ExportSessionSnapshot> sessions;
        if ("all_conversations".equals(scope)) {
            sessions = listOfMaps(agentClient.listSessions(maxSessions).get("sessions"))
                    .stream()
                    .limit(maxSessions)
                    .map(session -> loadSessionSnapshot(text(session.get("id")), scope, maxTurns, includeTimeline, includeEvidence, session))
                    .toList();
        } else {
            int turnLimit = "latest_turn".equals(scope) ? 1 : maxTurns;
            sessions = List.of(loadSessionSnapshot(sessionId, scope, turnLimit, includeTimeline, includeEvidence, Map.of()));
        }

        if (sessions.isEmpty() || sessions.get(0).turns().isEmpty()) {
            return loadSessionOnlySnapshot(sessionId, exportPlan, scope, includeEvidence, includeTimeline);
        }

        ExportSessionSnapshot firstSession = sessions.get(0);
        ExportTurnSnapshot firstTurn = firstSession.turns().get(0);
        String runId = requestedRunId.isBlank() ? firstTurn.runId() : requestedRunId;

        return new ExportSnapshot(
                sessionId,
                runId,
                scope,
                validateTitle(firstNonBlank(text(exportPlan.get("title")), "虚拟专家研判报告")),
                text(exportPlan.get("audience")),
                text(exportPlan.get("purpose")),
                includeEvidence,
                includeTimeline,
                listOfMaps(exportPlan.get("tables")),
                firstTurn.userMessage(),
                firstTurn.userMessageCreatedAt(),
                firstTurn.riskLevel(),
                firstTurn.judgment(),
                firstTurn.actions(),
                firstTurn.openQuestions(),
                firstTurn.events(),
                sessions
        );
    }

    private ExportSessionSnapshot loadSessionSnapshot(
            String sessionId,
            String scope,
            int maxTurns,
            boolean includeTimeline,
            boolean includeEvidence,
            Map<String, Object> sessionMeta
    ) {
        List<Map<String, Object>> timelineItems = new ArrayList<>();
        String beforeCursor = null;
        boolean hasMore;
        do {
            Map<String, Object> timeline = agentClient.listTimeline(sessionId, beforeCursor, Math.min(maxTurns, 20));
            List<Map<String, Object>> items = listOfMaps(timeline.get("items"));
            timelineItems.addAll(0, items);
            hasMore = booleanValue(firstNonNull(timeline.get("hasMoreBefore"), timeline.get("has_more_before")), false);
            beforeCursor = text(firstNonNull(timeline.get("beforeCursor"), timeline.get("before_cursor")));
        } while (hasMore && !beforeCursor.isBlank() && timelineItems.size() < maxTurns && !"latest_turn".equals(scope));

        if (timelineItems.size() > maxTurns) {
            timelineItems = timelineItems.subList(timelineItems.size() - maxTurns, timelineItems.size());
        }

        List<ExportTurnSnapshot> turns = timelineItems.stream()
                .map(item -> turnSnapshot(sessionId, item, includeTimeline, includeEvidence))
                .toList();

        return new ExportSessionSnapshot(
                sessionId,
                firstNonBlank(text(sessionMeta.get("title")), turns.isEmpty() ? "虚拟专家研判" : turns.get(0).userMessage()),
                text(sessionMeta.get("status")),
                text(sessionMeta.get("summary")),
                firstNonBlank(text(sessionMeta.get("updatedAt")), text(sessionMeta.get("updated_at"))),
                turns
        );
    }

    private ExportTurnSnapshot turnSnapshot(String sessionId, Map<String, Object> item, boolean includeTimeline, boolean includeEvidence) {
        Map<String, Object> userMessage = map(item.get("userMessage"));
        Map<String, Object> run = map(item.get("run"));
        String runId = text(run.get("id"));
        Map<String, Object> summary = map(run.get("summary"));
        List<Map<String, Object>> events = includeTimeline && !runId.isBlank()
                ? visibleEvents(listOfMaps(agentClient.listRunEvents(sessionId, runId, 0L).get("events")), includeEvidence)
                : List.of();
        return new ExportTurnSnapshot(
                text(userMessage.get("id")),
                text(userMessage.get("content")),
                text(userMessage.get("createdAt")),
                runId,
                text(run.get("status")),
                text(summary.get("riskLevel")),
                firstNonBlank(text(summary.get("judgment")), text(summary.get("finalAnswer"))),
                strings(summary.get("recommendedActions")),
                strings(summary.get("openQuestions")),
                events
        );
    }

    private ExportSnapshot loadSessionOnlySnapshot(
            String sessionId,
            Map<String, Object> exportPlan,
            String scope,
            boolean includeEvidence,
            boolean includeTimeline
    ) {
        Map<String, Object> response = agentClient.listSessions(100);
        Map<String, Object> session = listOfMaps(response.get("sessions")).stream()
                .filter(item -> sessionId.equals(text(item.get("id"))))
                .findFirst()
                .orElse(Map.of("id", sessionId, "title", "虚拟专家研判", "summary", "等待 Agent 生成分析结论"));
        ExportSessionSnapshot sessionSnapshot = new ExportSessionSnapshot(
                sessionId,
                text(session.get("title")),
                text(session.get("status")),
                text(session.get("summary")),
                text(session.get("updated_at")),
                List.of()
        );
        return new ExportSnapshot(
                sessionId,
                "",
                scope,
                validateTitle(firstNonBlank(text(exportPlan.get("title")), "虚拟专家研判报告")),
                text(exportPlan.get("audience")),
                text(exportPlan.get("purpose")),
                includeEvidence,
                includeTimeline,
                listOfMaps(exportPlan.get("tables")),
                text(session.get("title")),
                text(session.get("updated_at")),
                "",
                text(session.get("summary")),
                List.of(),
                List.of("该历史会话缺少多轮消息明细，仅导出会话摘要。"),
                List.of(),
                List.of(sessionSnapshot)
        );
    }

    private ExportFile writePdf(ExportSnapshot snapshot) throws IOException {
        String exportId = "exp_" + UUID.randomUUID().toString().replace("-", "");
        String fileName = "virtual-expert-" + FILE_TIME.format(LocalDateTime.now()) + ".pdf";
        Path path = exportRoot().resolve(exportId + ".pdf");

        try (PDDocument document = new PDDocument()) {
            PDFont font = loadChineseFont(document);
            PdfWriter writer = new PdfWriter(document, font);
            writer.line(snapshot.title(), 18, true);
            writer.blank();
            writer.line("会话编号：" + snapshot.sessionId(), 10, false);
            writer.line("运行编号：" + snapshot.runId(), 10, false);
            if (!snapshot.audience().isBlank()) {
                writer.line("面向对象：" + snapshot.audience(), 10, false);
            }
            if (!snapshot.purpose().isBlank()) {
                writer.line("导出用途：" + snapshot.purpose(), 10, false);
            }
            writer.line("生成时间：" + LocalDateTime.now(), 10, false);
            writer.blank();
            writer.section("一、用户问题");
            writer.paragraph(snapshot.userMessage());
            writer.section("二、风险判断");
            writer.paragraph(riskLabel(snapshot.riskLevel()) + "。" + snapshot.judgment());
            writer.section("三、处置建议");
            writer.list(snapshot.actions());
            writer.section("四、待补充信息");
            writer.list(snapshot.openQuestions());
            if (snapshot.includeTimeline()) {
                writer.section("五、过程摘要");
                writer.list(eventSummaries(snapshot.events(), snapshot.includeEvidence()));
            }
            writer.close();
            document.save(path.toFile());
        }

        return remember(exportId, fileName, "application/pdf", path);
    }

    private ExportFile writeExcel(ExportSnapshot snapshot) throws IOException {
        String exportId = "exp_" + UUID.randomUUID().toString().replace("-", "");
        String fileName = "virtual-expert-" + FILE_TIME.format(LocalDateTime.now()) + ".xlsx";
        Path path = exportRoot().resolve(exportId + ".xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle header = workbook.createCellStyle();
            header.setFont(boldFont);

            Sheet overview = workbook.createSheet(sheetName("会话信息", "会话信息"));
            writePairs(overview, header, List.of(
                    Map.entry("报告标题", snapshot.title()),
                    Map.entry("导出范围", scopeLabel(snapshot.scope())),
                    Map.entry("会话编号", snapshot.sessionId()),
                    Map.entry("运行编号", snapshot.runId()),
                    Map.entry("面向对象", snapshot.audience()),
                    Map.entry("导出用途", snapshot.purpose()),
                    Map.entry("用户问题", snapshot.userMessage()),
                    Map.entry("风险等级", riskLabel(snapshot.riskLevel())),
                    Map.entry("风险判断", snapshot.judgment())
            ));

            Sheet actions = workbook.createSheet(sheetName("结论建议", "结论建议"));
            writeRows(actions, header, "建议", snapshot.actions());

            Sheet questions = workbook.createSheet(sheetName("待补充信息", "待补充信息"));
            writeRows(questions, header, "信息项", snapshot.openQuestions());

            Sheet sessionOverview = workbook.createSheet(sheetName("会话汇总", "会话汇总"));
            writeSessionOverview(sessionOverview, header, snapshot.sessions());

            Sheet turnDetail = workbook.createSheet(sheetName("轮次明细", "轮次明细"));
            writeTurnDetails(turnDetail, header, snapshot.sessions());

            List<Sheet> sheets = new ArrayList<>(List.of(overview, actions, questions, sessionOverview, turnDetail));
            if (snapshot.includeTimeline()) {
                Sheet events = workbook.createSheet(sheetName("事件时间线", "事件时间线"));
                Row headerRow = events.createRow(0);
                List.of("序号", "会话编号", "运行编号", "类型", "摘要").forEach(title -> {
                    int index = headerRow.getLastCellNum() < 0 ? 0 : headerRow.getLastCellNum();
                    headerRow.createCell(index).setCellValue(title);
                    headerRow.getCell(index).setCellStyle(header);
                });
                writeEventRows(events, snapshot.sessions());
                sheets.add(events);
            }
            for (Map<String, Object> table : snapshot.tables()) {
                String type = text(table.get("type"));
                String title = sheetName(text(table.get("title")), tableTitle(type));
                if (workbook.getSheet(title) != null) {
                    continue;
                }
                Sheet sheet = workbook.createSheet(title);
                if ("action_list".equals(type)) {
                    writeRows(sheet, header, "处置建议", snapshot.actions());
                } else if ("evidence_list".equals(type)) {
                    writeRows(sheet, header, "证据摘要", eventSummaries(snapshot.events(), true));
                } else {
                    writePairs(sheet, header, List.of(
                            Map.entry("风险等级", riskLabel(snapshot.riskLevel())),
                            Map.entry("风险判断", snapshot.judgment())
                    ));
                }
                sheets.add(sheet);
            }
            autosize(sheets.toArray(Sheet[]::new));
            workbook.write(Files.newOutputStream(path));
        }

        return remember(exportId, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", path);
    }

    private ExportFile writeText(ExportSnapshot snapshot) throws IOException {
        String exportId = "exp_" + UUID.randomUUID().toString().replace("-", "");
        String fileName = "virtual-expert-" + FILE_TIME.format(LocalDateTime.now()) + ".txt";
        Path path = exportRoot().resolve(exportId + ".txt");
        Files.writeString(path, plainReport(snapshot), StandardCharsets.UTF_8);
        return remember(exportId, fileName, "text/plain;charset=UTF-8", path);
    }

    private ExportFile writeMarkdown(ExportSnapshot snapshot) throws IOException {
        String exportId = "exp_" + UUID.randomUUID().toString().replace("-", "");
        String fileName = "virtual-expert-" + FILE_TIME.format(LocalDateTime.now()) + ".md";
        Path path = exportRoot().resolve(exportId + ".md");
        Files.writeString(path, markdownReport(snapshot), StandardCharsets.UTF_8);
        return remember(exportId, fileName, "text/markdown;charset=UTF-8", path);
    }

    private ExportFile writeDocx(ExportSnapshot snapshot) throws IOException {
        String exportId = "exp_" + UUID.randomUUID().toString().replace("-", "");
        String fileName = "virtual-expert-" + FILE_TIME.format(LocalDateTime.now()) + ".docx";
        Path path = exportRoot().resolve(exportId + ".docx");

        try (XWPFDocument document = new XWPFDocument()) {
            addDocxParagraph(document, snapshot.title(), true, 18);
            addDocxParagraph(document, "会话编号：" + snapshot.sessionId(), false, 10);
            addDocxParagraph(document, "运行编号：" + snapshot.runId(), false, 10);
            if (!snapshot.audience().isBlank()) {
                addDocxParagraph(document, "面向对象：" + snapshot.audience(), false, 10);
            }
            if (!snapshot.purpose().isBlank()) {
                addDocxParagraph(document, "导出用途：" + snapshot.purpose(), false, 10);
            }
            addDocxParagraph(document, "生成时间：" + LocalDateTime.now(), false, 10);
            addDocxParagraph(document, "一、用户问题", true, 14);
            addDocxParagraph(document, snapshot.userMessage(), false, 11);
            addDocxParagraph(document, "二、风险判断", true, 14);
            addDocxParagraph(document, riskLabel(snapshot.riskLevel()) + "。" + snapshot.judgment(), false, 11);
            addDocxParagraph(document, "三、处置建议", true, 14);
            addDocxList(document, snapshot.actions());
            addDocxParagraph(document, "四、待补充信息", true, 14);
            addDocxList(document, snapshot.openQuestions());
            if (snapshot.includeTimeline()) {
                addDocxParagraph(document, "五、过程摘要", true, 14);
                addDocxList(document, eventSummaries(snapshot.events(), snapshot.includeEvidence()));
            }
            document.write(Files.newOutputStream(path));
        }

        return remember(exportId, fileName, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", path);
    }

    private void addDocxParagraph(XWPFDocument document, String text, boolean bold, int fontSize) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = paragraph.createRun();
        run.setText(firstNonBlank(text, "暂无"));
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("Microsoft YaHei");
    }

    private void addDocxList(XWPFDocument document, List<String> values) {
        for (String value : values.isEmpty() ? List.of("暂无") : values) {
            addDocxParagraph(document, "· " + value, false, 11);
        }
    }

    private String plainReport(ExportSnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append(snapshot.title()).append("\n");
        builder.append("会话编号：").append(snapshot.sessionId()).append("\n");
        builder.append("运行编号：").append(snapshot.runId()).append("\n");
        if (!snapshot.audience().isBlank()) {
            builder.append("面向对象：").append(snapshot.audience()).append("\n");
        }
        if (!snapshot.purpose().isBlank()) {
            builder.append("导出用途：").append(snapshot.purpose()).append("\n");
        }
        builder.append("生成时间：").append(LocalDateTime.now()).append("\n\n");
        builder.append("一、用户问题\n").append(snapshot.userMessage()).append("\n\n");
        builder.append("二、风险判断\n").append(riskLabel(snapshot.riskLevel())).append("。").append(snapshot.judgment()).append("\n\n");
        builder.append("三、处置建议\n").append(listText(snapshot.actions()));
        builder.append("\n四、待补充信息\n").append(listText(snapshot.openQuestions()));
        if (snapshot.includeTimeline()) {
            builder.append("\n五、过程摘要\n").append(listText(eventSummaries(snapshot.events(), snapshot.includeEvidence())));
        }
        return builder.toString();
    }

    private String markdownReport(ExportSnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(snapshot.title()).append("\n\n");
        builder.append("- 会话编号：").append(snapshot.sessionId()).append("\n");
        builder.append("- 运行编号：").append(snapshot.runId()).append("\n");
        if (!snapshot.audience().isBlank()) {
            builder.append("- 面向对象：").append(snapshot.audience()).append("\n");
        }
        if (!snapshot.purpose().isBlank()) {
            builder.append("- 导出用途：").append(snapshot.purpose()).append("\n");
        }
        builder.append("- 生成时间：").append(LocalDateTime.now()).append("\n\n");
        builder.append("## 一、用户问题\n\n").append(snapshot.userMessage()).append("\n\n");
        builder.append("## 二、风险判断\n\n").append(riskLabel(snapshot.riskLevel())).append("。").append(snapshot.judgment()).append("\n\n");
        builder.append("## 三、处置建议\n\n").append(markdownList(snapshot.actions()));
        builder.append("\n## 四、待补充信息\n\n").append(markdownList(snapshot.openQuestions()));
        if (snapshot.includeTimeline()) {
            builder.append("\n## 五、过程摘要\n\n").append(markdownList(eventSummaries(snapshot.events(), snapshot.includeEvidence())));
        }
        return builder.toString();
    }

    private String listText(List<String> values) {
        return String.join("\n", (values.isEmpty() ? List.of("暂无") : values).stream().map(value -> "· " + value).toList()) + "\n";
    }

    private String markdownList(List<String> values) {
        return String.join("\n", (values.isEmpty() ? List.of("暂无") : values).stream().map(value -> "- " + value).toList()) + "\n";
    }

    private ExportFile remember(String exportId, String fileName, String contentType, Path path) throws IOException {
        ExportFile file = new ExportFile(exportId, fileName, contentType, path, Files.size(path));
        files.put(exportId, file);
        return file;
    }

    private Path exportRoot() throws IOException {
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "pipeline-virtual-expert-exports");
        Files.createDirectories(root);
        return root;
    }

    private PDFont loadChineseFont(PDDocument document) throws IOException {
        for (String path : List.of("/mnt/c/Windows/Fonts/simhei.ttf", "/mnt/c/Windows/Fonts/msyh.ttc", "/mnt/c/Windows/Fonts/simsun.ttc")) {
            if (Files.exists(Path.of(path))) {
                return PDType0Font.load(document, Path.of(path).toFile());
            }
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private void writePairs(Sheet sheet, CellStyle header, List<Map.Entry<String, String>> rows) {
        for (int i = 0; i < rows.size(); i += 1) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(rows.get(i).getKey());
            row.createCell(1).setCellValue(rows.get(i).getValue());
            row.getCell(0).setCellStyle(header);
        }
    }

    private void writeRows(Sheet sheet, CellStyle header, String title, List<String> values) {
        Row head = sheet.createRow(0);
        head.createCell(0).setCellValue(title);
        head.getCell(0).setCellStyle(header);
        List<String> rows = values.isEmpty() ? List.of("暂无") : values;
        for (int i = 0; i < rows.size(); i += 1) {
            sheet.createRow(i + 1).createCell(0).setCellValue(rows.get(i));
        }
    }

    private void writeSessionOverview(Sheet sheet, CellStyle header, List<ExportSessionSnapshot> sessions) {
        Row head = sheet.createRow(0);
        List<String> titles = List.of("会话编号", "标题", "状态", "更新时间", "摘要", "轮次数");
        for (int i = 0; i < titles.size(); i += 1) {
            head.createCell(i).setCellValue(titles.get(i));
            head.getCell(i).setCellStyle(header);
        }
        for (int i = 0; i < sessions.size(); i += 1) {
            ExportSessionSnapshot session = sessions.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(session.sessionId());
            row.createCell(1).setCellValue(session.title());
            row.createCell(2).setCellValue(session.status());
            row.createCell(3).setCellValue(session.updatedAt());
            row.createCell(4).setCellValue(session.summary());
            row.createCell(5).setCellValue(session.turns().size());
        }
    }

    private void writeTurnDetails(Sheet sheet, CellStyle header, List<ExportSessionSnapshot> sessions) {
        Row head = sheet.createRow(0);
        List<String> titles = List.of("会话编号", "运行编号", "用户输入", "风险等级", "风险判断", "处置建议");
        for (int i = 0; i < titles.size(); i += 1) {
            head.createCell(i).setCellValue(titles.get(i));
            head.getCell(i).setCellStyle(header);
        }
        int rowIndex = 1;
        for (ExportSessionSnapshot session : sessions) {
            for (ExportTurnSnapshot turn : session.turns()) {
                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(session.sessionId());
                row.createCell(1).setCellValue(turn.runId());
                row.createCell(2).setCellValue(turn.userMessage());
                row.createCell(3).setCellValue(riskLabel(turn.riskLevel()));
                row.createCell(4).setCellValue(turn.judgment());
                row.createCell(5).setCellValue(String.join("\n", turn.actions()));
                rowIndex += 1;
            }
        }
    }

    private void writeEventRows(Sheet sheet, List<ExportSessionSnapshot> sessions) {
        int rowIndex = 1;
        for (ExportSessionSnapshot session : sessions) {
            for (ExportTurnSnapshot turn : session.turns()) {
                for (Map<String, Object> event : turn.events()) {
                    Row row = sheet.createRow(rowIndex);
                    row.createCell(0).setCellValue(rowIndex);
                    row.createCell(1).setCellValue(session.sessionId());
                    row.createCell(2).setCellValue(turn.runId());
                    row.createCell(3).setCellValue(text(event.get("type")));
                    row.createCell(4).setCellValue(eventCaption(event));
                    rowIndex += 1;
                }
            }
        }
    }

    private void autosize(Sheet... sheets) {
        for (Sheet sheet : sheets) {
            int columns = sheet.getRow(0) == null ? 0 : sheet.getRow(0).getLastCellNum();
            for (int index = 0; index < columns; index += 1) {
                sheet.autoSizeColumn(index);
            }
        }
    }

    private String normalizeFormat(String format) {
        String normalized = format.toLowerCase(Locale.ROOT);
        if ("xlsx".equals(normalized)) {
            return EXCEL;
        }
        if ("markdown".equals(normalized)) {
            return MARKDOWN;
        }
        if ("word".equals(normalized) || "doc".equals(normalized)) {
            return DOCX;
        }
        if (List.of(PDF, EXCEL, TXT, MARKDOWN, DOCX).contains(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported export format: " + format);
    }

    private boolean contentTypeMatches(String contentType, String format) {
        if (PDF.equals(format)) {
            return "application/pdf".equals(contentType);
        }
        if (TXT.equals(format)) {
            return contentType != null && contentType.startsWith("text/plain");
        }
        if (MARKDOWN.equals(format)) {
            return contentType != null && contentType.startsWith("text/markdown");
        }
        if (DOCX.equals(format)) {
            return contentType != null && contentType.contains("wordprocessingml");
        }
        return contentType != null && contentType.contains("spreadsheet");
    }

    private String validateTitle(String title) {
        String value = firstNonBlank(title, "虚拟专家研判报告")
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .trim();
        if (value.isBlank()) {
            return "虚拟专家研判报告";
        }
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private String sheetName(String name, String fallback) {
        String value = firstNonBlank(name, fallback)
                .replaceAll("[\\\\/?*\\[\\]:]", "-")
                .trim();
        if (value.isBlank()) {
            value = fallback;
        }
        return value.length() > 31 ? value.substring(0, 31) : value;
    }

    private String tableTitle(String type) {
        return switch (type) {
            case "action_list" -> "处置建议";
            case "evidence_list" -> "证据明细";
            case "risk_list" -> "风险清单";
            default -> "导出明细";
        };
    }

    private List<String> eventSummaries(List<Map<String, Object>> events, boolean includeEvidence) {
        List<String> summaries = new ArrayList<>();
        for (Map<String, Object> event : visibleEvents(events, includeEvidence)) {
            String type = text(event.get("type"));
            if (List.of("tool_completed", "knowledge_search_completed", "retrieval_completed", "memory_search_completed", "export_created").contains(type)) {
                summaries.add(type + "：" + eventCaption(event));
            }
        }
        return summaries.isEmpty() ? List.of("暂无关键过程摘要") : summaries;
    }

    private List<Map<String, Object>> visibleEvents(List<Map<String, Object>> events, boolean includeEvidence) {
        if (includeEvidence) {
            return events;
        }
        return events.stream()
                .filter(event -> !List.of("tool_completed", "knowledge_search_completed", "retrieval_completed").contains(text(event.get("type"))))
                .toList();
    }

    private String eventCaption(Map<String, Object> event) {
        Map<String, Object> payload = map(event.get("payload"));
        return firstNonBlank(
                text(payload.get("summary")),
                text(payload.get("judgment")),
                text(event.get("message")),
                text(event.get("title")),
                text(payload.get("tool_name"))
        );
    }

    private List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(item -> !item.isBlank()).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> ? new LinkedHashMap<>((Map<String, Object>) value) : Map.of();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    private int intValue(Object value, int defaultValue, int min, int max) {
        int number;
        if (value instanceof Number numeric) {
            number = numeric.intValue();
        } else {
            try {
                number = Integer.parseInt(text(value));
            } catch (NumberFormatException ignored) {
                number = defaultValue;
            }
        }
        return Math.max(min, Math.min(max, number));
    }

    private String riskLabel(String riskLevel) {
        return switch (riskLevel) {
            case "low" -> "低风险";
            case "medium" -> "中风险";
            case "high" -> "高风险";
            case "critical" -> "严重风险";
            default -> "未定级";
        };
    }

    private String scopeLabel(String scope) {
        return switch (scope) {
            case "latest_turn" -> "最后一轮";
            case "full_session" -> "当前会话全部";
            case "all_conversations" -> "全部会话历史";
            default -> "Agent 自动筛选";
        };
    }

    private record ExportSnapshot(
            String sessionId,
            String runId,
            String scope,
            String title,
            String audience,
            String purpose,
            boolean includeEvidence,
            boolean includeTimeline,
            List<Map<String, Object>> tables,
            String userMessage,
            String userMessageCreatedAt,
            String riskLevel,
            String judgment,
            List<String> actions,
            List<String> openQuestions,
            List<Map<String, Object>> events,
            List<ExportSessionSnapshot> sessions
    ) {
    }

    private record ExportSessionSnapshot(
            String sessionId,
            String title,
            String status,
            String summary,
            String updatedAt,
            List<ExportTurnSnapshot> turns
    ) {
    }

    private record ExportTurnSnapshot(
            String messageId,
            String userMessage,
            String userMessageCreatedAt,
            String runId,
            String runStatus,
            String riskLevel,
            String judgment,
            List<String> actions,
            List<String> openQuestions,
            List<Map<String, Object>> events
    ) {
    }

    private static class PdfWriter {
        private final PDDocument document;
        private final PDFont font;
        private PDPageContentStream stream;
        private float y;

        PdfWriter(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            addPage();
        }

        void section(String text) throws IOException {
            blank();
            line(text, 13, true);
        }

        void paragraph(String text) throws IOException {
            for (String line : wrap(text, 42)) {
                line(line, 10, false);
            }
        }

        void list(List<String> items) throws IOException {
            List<String> rows = items.isEmpty() ? List.of("暂无") : items;
            for (String item : rows) {
                for (String line : wrap("· " + item, 44)) {
                    line(line, 10, false);
                }
            }
        }

        void blank() throws IOException {
            ensureSpace(12);
            y -= 10;
        }

        void line(String text, int size, boolean bold) throws IOException {
            ensureSpace(size + 8);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(54, y);
            stream.showText(sanitize(text));
            stream.endText();
            y -= size + (bold ? 10 : 7);
        }

        private void addPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - 54;
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < 54) {
                addPage();
            }
        }

        private List<String> wrap(String text, int width) {
            String value = text == null || text.isBlank() ? "暂无" : text;
            List<String> rows = new ArrayList<>();
            for (int start = 0; start < value.length(); start += width) {
                rows.add(value.substring(start, Math.min(start + width, value.length())));
            }
            return rows;
        }

        private String sanitize(String value) {
            return value.replace('\n', ' ').replace('\r', ' ');
        }
    }
}
