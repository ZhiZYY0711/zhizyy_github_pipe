package com.gxa.pipe.virtualExpert.agent;

import com.gxa.pipe.virtualExpert.agent.dto.AgentRunResponse;
import com.gxa.pipe.virtualExpert.agent.dto.AgentSessionResponse;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AgentClientTest {

    private final RestTemplate restTemplate = new RestTemplateBuilder().build();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    private final AgentClient client = new AgentClient(
            new AgentServiceProperties("http://agent.local", "dev-agent-secret", "pipeline-agent"),
            restTemplate
    );

    @Test
    void createSessionSendsSnakeCasePayloadAndReadsSnakeCaseResponse() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> payload = new AtomicReference<>();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/api/agent/sessions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {
                      "session_id": "ana_001",
                      "status": "created"
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        httpServer.start();
        AgentClient localClient = new AgentClient(
                new AgentServiceProperties("http://127.0.0.1:" + httpServer.getAddress().getPort(), "dev-agent-secret", "pipeline-agent"),
                new RestTemplateBuilder().build()
        );

        AgentSessionResponse response;
        try {
            response = localClient.createSession(
                    Map.of("rawInput", "pressure anomaly", "sourceType", "manual")
            );
        } finally {
            httpServer.stop(0);
        }

        assertThat(response.sessionId()).isEqualTo("ana_001");
        assertThat(response.status()).isEqualTo("created");
        assertThat(payload.get()).contains("\"raw_input\":\"pressure anomaly\"");
        assertThat(payload.get()).contains("\"source_type\":\"manual\"");
        assertThat(authorization.get())
                .startsWith("Bearer ")
                .matches("Bearer [^.]+\\.[^.]+\\.[^.]+");
        Jwts.parser()
                .setSigningKey("dev-agent-secret".getBytes(StandardCharsets.UTF_8))
                .requireAudience("pipeline-agent")
                .parseClaimsJws(authorization.get().substring("Bearer ".length()));
    }

    @Test
    void runSessionReadsRunIdAndEvents() {
        server.expect(once(), requestTo("http://agent.local/api/agent/sessions/ana_001/runs"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "session_id": "ana_001",
                          "run_id": "run_001",
                          "events": [{
                            "session_id": "ana_001",
                            "run_id": "run_001",
                            "created_at": "2026-04-23T10:00:00Z",
                            "type": "plan_created"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        AgentRunResponse response = client.runSession("ana_001");

        assertThat(response.sessionId()).isEqualTo("ana_001");
        assertThat(response.runId()).isEqualTo("run_001");
        assertThat(response.events()).isEqualTo(List.of(Map.of(
                "sessionId", "ana_001",
                "runId", "run_001",
                "createdAt", "2026-04-23T10:00:00Z",
                "type", "plan_created"
        )));
        server.verify();
    }

    @Test
    void listSessionsForwardsLimit() {
        server.expect(once(), requestTo("http://agent.local/api/agent/sessions?limit=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "sessions": [{
                            "id": "ana_001",
                            "title": "压力异常",
                            "status": "completed"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response = client.listSessions(10);

        assertThat((List<?>) response.get("sessions")).hasSize(1);
        server.verify();
    }

    @Test
    void deleteSessionForwardsDeleteAndReturnsPayload() {
        server.expect(once(), requestTo("http://agent.local/api/agent/sessions/ana_001"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("""
                        {
                          "session_id": "ana_001",
                          "status": "deleted"
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response = client.deleteSession("ana_001");

        assertThat(response).containsEntry("session_id", "ana_001");
        assertThat(response).containsEntry("status", "deleted");
        server.verify();
    }

    @Test
    void createMessageSendsContentAndNormalizesRunResponse() throws Exception {
        AtomicReference<String> payload = new AtomicReference<>();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/api/agent/sessions/ana_001/messages", exchange -> {
            payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {
                      "message": {
                        "id": "msg_001",
                        "role": "user",
                        "content": "继续追问",
                        "messageType": "text",
                        "createdAt": "2026-04-26T10:00:00Z"
                      },
                      "run": {
                        "id": "run_001",
                        "sessionId": "ana_001",
                        "status": "created",
                        "streamUrl": "/manager/virtual-expert/agent/sessions/ana_001/runs/run_001/stream"
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        httpServer.start();
        AgentClient localClient = new AgentClient(
                new AgentServiceProperties("http://127.0.0.1:" + httpServer.getAddress().getPort(), "dev-agent-secret", "pipeline-agent"),
                new RestTemplateBuilder().build()
        );

        Map<String, Object> response;
        try {
            response = localClient.createMessage("ana_001", Map.of("content", "继续追问"));
        } finally {
            httpServer.stop(0);
        }

        assertThat(response).extracting("message").isInstanceOf(Map.class);
        assertThat(payload.get()).contains("\"content\":\"继续追问\"");
        assertThat(payload.get()).contains("\"message_type\":\"text\"");
        @SuppressWarnings("unchecked")
        Map<String, Object> run = (Map<String, Object>) response.get("run");
        assertThat(run).containsEntry("id", "run_001");
    }

    @Test
    void createMessageNormalizesExportPlanResponse() throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/api/agent/sessions/ana_001/messages", exchange -> {
            byte[] response = """
                    {
                      "message": {
                        "id": "msg_001",
                        "role": "user",
                        "content": "请导出 PDF",
                        "messageType": "text",
                        "createdAt": "2026-04-27T01:00:00Z"
                      },
                      "export_plan": {
                        "format": "pdf",
                        "requiresConfirmation": false
                      },
                      "run": null
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        httpServer.start();
        AgentClient localClient = new AgentClient(
                new AgentServiceProperties("http://127.0.0.1:" + httpServer.getAddress().getPort(), "dev-agent-secret", "pipeline-agent"),
                new RestTemplateBuilder().build()
        );

        Map<String, Object> response;
        try {
            response = localClient.createMessage("ana_001", Map.of("content", "请导出 PDF"));
        } finally {
            httpServer.stop(0);
        }

        assertThat(response).containsKey("exportPlan");
        assertThat(response).doesNotContainKey("export_plan");
        assertThat(response).containsEntry("run", null);
    }

    @Test
    void createMessagePreservesUpstreamConflictPayload() throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/api/agent/sessions/ana_001/messages", exchange -> {
            byte[] response = """
                    {
                      "code": "SESSION_RUN_IN_PROGRESS",
                      "message": "当前会话仍有研判在运行，请等待完成或取消后继续追问。",
                      "sessionId": "ana_001",
                      "runId": "run_001"
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(409, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        httpServer.start();
        AgentClient localClient = new AgentClient(
                new AgentServiceProperties("http://127.0.0.1:" + httpServer.getAddress().getPort(), "dev-agent-secret", "pipeline-agent"),
                new RestTemplateBuilder().build()
        );

        try {
            assertThatThrownBy(() -> localClient.createMessage("ana_001", Map.of("content", "继续追问")))
                    .isInstanceOfSatisfying(AgentServiceException.class, exception -> {
                        assertThat(exception.statusCode()).isEqualTo(409);
                        assertThat(exception.body()).containsEntry("code", "SESSION_RUN_IN_PROGRESS");
                    });
        } finally {
            httpServer.stop(0);
        }
    }

    @Test
    void listTimelineNormalizesNestedSnakeCaseFields() {
        server.expect(once(), requestTo("http://agent.local/api/agent/sessions/ana_001/timeline?beforeCursor=&limit=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "sessionId": "ana_001",
                          "items": [{
                            "turnId": "msg_001",
                            "cursor": "c1",
                            "userMessage": {
                              "id": "msg_001",
                              "content": "第一轮",
                              "createdAt": "2026-04-26T10:00:00Z"
                            },
                            "run": {
                              "id": "run_001",
                              "status": "completed",
                              "summary": {
                                "finalAnswer": "建议复核阀门",
                                "riskLevel": "medium"
                              },
                              "eventCount": 3,
                              "createdAt": "2026-04-26T10:00:01Z",
                              "completedAt": "2026-04-26T10:00:03Z"
                            }
                          }],
                          "hasMoreBefore": false,
                          "beforeCursor": "c1"
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response = client.listTimeline("ana_001", null, 1);

        assertThat(response).containsEntry("sessionId", "ana_001");
        assertThat((List<?>) response.get("items")).hasSize(1);
        server.verify();
    }

    @Test
    void listTimelineEncodesCursorTimezonePlusSign() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/api/agent/sessions/ana_001/timeline", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] response = """
                    {
                      "sessionId": "ana_001",
                      "items": [],
                      "hasMoreBefore": false,
                      "beforeCursor": null
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        httpServer.start();
        AgentClient localClient = new AgentClient(
                new AgentServiceProperties("http://127.0.0.1:" + httpServer.getAddress().getPort(), "dev-agent-secret", "pipeline-agent"),
                new RestTemplateBuilder().build()
        );

        try {
            localClient.listTimeline("ana_001", "2026-04-26T17:08:48.140162+00:00|msg_001", 5);
        } finally {
            httpServer.stop(0);
        }

        assertThat(query.get()).contains("beforeCursor=2026-04-26T17%3A08%3A48.140162%2B00%3A00%7Cmsg_001");
        assertThat(query.get()).contains("limit=5");
    }

    @Test
    void updateSessionSendsPatchBodyViaHttpClient() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> payload = new AtomicReference<>();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/api/agent/sessions/ana_001", exchange -> {
            method.set(exchange.getRequestMethod());
            payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {
                      "session_id": "ana_001",
                      "title": "新标题",
                      "pinned": true
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        httpServer.start();
        AgentClient localClient = new AgentClient(
                new AgentServiceProperties("http://127.0.0.1:" + httpServer.getAddress().getPort(), "dev-agent-secret", "pipeline-agent"),
                new RestTemplateBuilder().build()
        );

        Map<String, Object> response;
        try {
            response = localClient.updateSession("ana_001", Map.of("title", "新标题", "pinned", true));
        } finally {
            httpServer.stop(0);
        }

        assertThat(method.get()).isEqualTo("PATCH");
        assertThat(payload.get()).contains("\"title\":\"新标题\"");
        assertThat(payload.get()).contains("\"pinned\":true");
        assertThat(response).containsEntry("session_id", "ana_001");
        assertThat(response).containsEntry("title", "新标题");
        assertThat(response).containsEntry("pinned", true);
    }

    @Test
    void clientAbortDetectionTreatsBrokenPipeAsDisconnect() {
        IOException exception = new IOException("ServletOutputStream failed to flush: java.io.IOException: Broken pipe");

        assertThat(AgentClient.isClientAbort(exception)).isTrue();
    }

    @Test
    void clientAbortDetectionTreatsNestedRuntimeBrokenPipeAsDisconnect() {
        RuntimeException exception = new RuntimeException(
                "Servlet container error notification for disconnected client",
                new IOException("Broken pipe")
        );

        assertThat(AgentClient.isClientAbort(exception)).isTrue();
    }
}
