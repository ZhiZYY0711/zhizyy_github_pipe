package com.gxa.pipe.virtualExpert.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxa.pipe.virtualExpert.agent.dto.AgentRunResponse;
import com.gxa.pipe.virtualExpert.agent.dto.AgentSessionResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class AgentClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentServiceProperties properties;
    private final RestTemplate restTemplate;
    private final HttpClient httpClient;

    @Autowired
    public AgentClient(AgentServiceProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this(properties, restTemplateBuilder.build(), HttpClient.newHttpClient());
    }

    AgentClient(AgentServiceProperties properties, RestTemplate restTemplate) {
        this(properties, restTemplate, HttpClient.newHttpClient());
    }

    AgentClient(AgentServiceProperties properties, RestTemplate restTemplate, HttpClient httpClient) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.httpClient = httpClient;
    }

    public AgentSessionResponse createSession(Map<String, Object> request) {
        return postJson(url("/api/agent/sessions"), toJson(toAgentPayload(request)), AgentSessionResponse.class);
    }

    public Map<String, Object> listSessions(Integer limit) {
        Map<String, Object> response = restTemplate.exchange(
                url("/api/agent/sessions?limit={limit}"),
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                },
                limit == null ? 20 : limit
        ).getBody();
        return response == null ? Map.of("sessions", java.util.List.of()) : response;
    }

    public Map<String, Object> deleteSession(String sessionId) {
        Map<String, Object> response = restTemplate.exchange(
                url("/api/agent/sessions/{sessionId}"),
                HttpMethod.DELETE,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                },
                sessionId
        ).getBody();
        return response == null ? Map.of("session_id", sessionId, "status", "deleted") : response;
    }

    public AgentRunResponse runSession(String sessionId) {
        AgentRunResponse response = restTemplate.postForObject(
                url("/api/agent/sessions/{sessionId}/runs"),
                new HttpEntity<>(headers()),
                AgentRunResponse.class,
                sessionId
        );
        return normalizeEvents(response);
    }

    public Map<String, Object> createMessage(String sessionId, Map<String, Object> request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", firstValue(request, "content", "content"));
        Object messageType = firstValue(request, "messageType", "message_type");
        payload.put("message_type", messageType == null ? "text" : messageType);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = postJson(
                url("/api/agent/sessions/" + sessionId + "/messages"),
                toJson(payload),
                Map.class
        );
        if (response == null) {
            return Map.of();
        }
        rename(response, "export_plan", "exportPlan");
        return response;
    }

    public StreamingResponseBody streamRunSession(String sessionId) {
        return outputStream -> {
            log.info("Agent stream proxy starting for session {}", sessionId);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url("/api/agent/sessions/" + sessionId + "/runs/stream")))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateInternalToken())
                    .header("X-Agent-Audience", properties.getAudience())
                    .GET()
                    .build();
            try {
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                log.info("Agent stream proxy response status {} for session {}", response.statusCode(), sessionId);
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Agent stream request failed: " + response.statusCode());
                }
                try (InputStream body = response.body()) {
                    copyStream(body, outputStream);
                }
                log.info("Agent stream proxy completed for session {}", sessionId);
            } catch (IOException e) {
                if (isClientAbort(e)) {
                    log.debug("Agent stream client disconnected for session {}", sessionId);
                    return;
                }
                writeStreamError(outputStream, "Agent stream request failed");
            } catch (RuntimeException e) {
                if (isClientAbort(e)) {
                    log.debug("Agent stream client disconnected for session {}", sessionId);
                    return;
                }
                writeStreamError(outputStream, e.getMessage() == null ? "Agent stream request failed" : e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                writeStreamError(outputStream, "Agent stream request interrupted");
            }
        };
    }

    public StreamingResponseBody streamRunSession(String sessionId, String runId) {
        return outputStream -> {
            log.info("Agent stream proxy starting for session {} run {}", sessionId, runId);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url("/api/agent/sessions/" + sessionId + "/runs/" + runId + "/stream")))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateInternalToken())
                    .header("X-Agent-Audience", properties.getAudience())
                    .GET()
                    .build();
            try {
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                log.info("Agent stream proxy response status {} for session {} run {}", response.statusCode(), sessionId, runId);
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Agent stream request failed: " + response.statusCode());
                }
                try (InputStream body = response.body()) {
                    copyStream(body, outputStream);
                }
                log.info("Agent stream proxy completed for session {} run {}", sessionId, runId);
            } catch (IOException e) {
                if (isClientAbort(e)) {
                    log.debug("Agent stream client disconnected for session {} run {}", sessionId, runId);
                    return;
                }
                writeStreamError(outputStream, "Agent stream request failed");
            } catch (RuntimeException e) {
                if (isClientAbort(e)) {
                    log.debug("Agent stream client disconnected for session {} run {}", sessionId, runId);
                    return;
                }
                writeStreamError(outputStream, e.getMessage() == null ? "Agent stream request failed" : e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                writeStreamError(outputStream, "Agent stream request interrupted");
            }
        };
    }

    public Map<String, Object> listEvents(String sessionId, Long afterSeq) {
        String url = url("/api/agent/sessions/{sessionId}/events?after_seq={afterSeq}");
        Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                },
                sessionId,
                afterSeq == null ? 0 : afterSeq
        ).getBody();
        return normalizeEventEnvelope(response);
    }

    public Map<String, Object> listRunEvents(String sessionId, String runId, Long afterSeq) {
        String url = url("/api/agent/sessions/{sessionId}/runs/{runId}/events?afterSeq={afterSeq}");
        Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                },
                sessionId,
                runId,
                afterSeq == null ? 0 : afterSeq
        ).getBody();
        return normalizeEventEnvelope(response);
    }

    public Map<String, Object> listTimeline(String sessionId, String beforeCursor, Integer limit) {
        URI target = URI.create(url("/api/agent/sessions/" + sessionId
                + "/timeline?beforeCursor=" + encodeQueryValue(beforeCursor == null ? "" : beforeCursor)
                + "&limit=" + (limit == null ? 1 : limit)));
        Map<String, Object> response = restTemplate.exchange(
                target,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        ).getBody();
        return response == null ? Map.of() : response;
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public Map<String, Object> cancelRun(String sessionId, String runId) {
        return restTemplate.exchange(
                url("/api/agent/sessions/{sessionId}/runs/{runId}/cancel"),
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                },
                sessionId,
                runId
        ).getBody();
    }

    private Map<String, Object> toAgentPayload(Map<String, Object> request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("raw_input", firstValue(request, "rawInput", "raw_input"));
        payload.put("title", firstValue(request, "title", "title"));
        payload.put("source_type", firstValue(request, "sourceType", "source_type"));
        payload.put("source_id", firstValue(request, "sourceId", "source_id"));
        payload.put("object_type", firstValue(request, "objectType", "object_type"));
        payload.put("object_id", firstValue(request, "objectId", "object_id"));
        payload.put("object_name", firstValue(request, "objectName", "object_name"));
        return payload;
    }

    private Object firstValue(Map<String, Object> values, String camelCaseKey, String snakeCaseKey) {
        Object value = values.get(camelCaseKey);
        return value != null ? value : values.get(snakeCaseKey);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize agent request", e);
        }
    }

    private <T> T postJson(String targetUrl, String body, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(targetUrl))
                .version(HttpClient.Version.HTTP_1_1)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateInternalToken())
                .header("X-Agent-Audience", properties.getAudience())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AgentServiceException(response.statusCode(), parseErrorBody(response.body()));
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new IllegalStateException("Agent request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Agent request interrupted", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseErrorBody(String responseBody) {
        try {
            Object parsed = objectMapper.readValue(responseBody, Object.class);
            if (parsed instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (IOException ignored) {
            // Fall through to a generic payload below.
        }
        return Map.of("message", responseBody == null || responseBody.isBlank() ? "Agent request failed" : responseBody);
    }

    private AgentRunResponse normalizeEvents(AgentRunResponse response) {
        if (response == null) {
            return null;
        }
        return new AgentRunResponse(response.sessionId(), response.runId(), normalizeEventList(response.events()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeEventEnvelope(Map<String, Object> response) {
        if (response == null) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("sessionId", response.get("session_id"));
        normalized.put("runId", response.get("run_id"));
        Object events = response.get("events");
        normalized.put(
                "events",
                events instanceof java.util.List<?> list
                        ? normalizeEventList((java.util.List<Map<String, Object>>) list)
                        : java.util.List.of()
        );
        return normalized;
    }

    private java.util.List<Map<String, Object>> normalizeEventList(java.util.List<Map<String, Object>> events) {
        if (events == null) {
            return java.util.List.of();
        }
        return events.stream().map(this::normalizeEvent).toList();
    }

    private Map<String, Object> normalizeEvent(Map<String, Object> event) {
        Map<String, Object> normalized = new LinkedHashMap<>(event);
        rename(normalized, "session_id", "sessionId");
        rename(normalized, "run_id", "runId");
        rename(normalized, "created_at", "createdAt");
        return normalized;
    }

    private void rename(Map<String, Object> values, String source, String target) {
        if (values.containsKey(source)) {
            values.put(target, values.remove(source));
        }
    }

    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, length);
            outputStream.flush();
        }
    }

    private void writeStreamError(OutputStream outputStream, String message) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "run_failed",
                    "level", "error",
                    "message", message
            );
            outputStream.write(("event: agent_error\ndata: "
                    + objectMapper.writeValueAsString(payload)
                    + "\n\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException writeFailure) {
            if (isClientAbort(writeFailure)) {
                log.debug("Agent stream client disconnected while writing error event");
                return;
            }
            log.warn("Failed to write agent stream error event: {}", writeFailure.getMessage());
        }
    }

    static boolean isClientAbort(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();
            if (className.contains("ClientAbortException")
                    || className.contains("AsyncRequestNotUsableException")
                    || message != null && message.contains("Broken pipe")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateInternalToken());
        headers.set("X-Agent-Audience", properties.getAudience());
        return headers;
    }

    private String generateInternalToken() {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject("server-web")
                .setAudience(properties.getAudience())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 5 * 60 * 1000L))
                .signWith(SignatureAlgorithm.HS256, properties.getInternalJwtSecret().getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    private String url(String path) {
        return properties.getBaseUrl().replaceAll("/+$", "") + path;
    }
}
