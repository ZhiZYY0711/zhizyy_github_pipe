package com.gxa.pipe.virtualExpert.agent;

import com.gxa.pipe.virtualExpert.agent.dto.AgentRunResponse;
import com.gxa.pipe.virtualExpert.agent.dto.AgentSessionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/virtual-expert/agent", "/manager/virtual-expert/agent"})
public class VirtualExpertAgentController {

    private final AgentClient agentClient;
    private final VirtualExpertExportService exportService;

    @ExceptionHandler(AgentServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAgentServiceException(AgentServiceException exception) {
        return ResponseEntity
                .status(HttpStatusCode.valueOf(exception.statusCode()))
                .body(exception.body());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", "INVALID_EXPORT_REQUEST",
                "message", exception.getMessage()
        ));
    }

    @GetMapping("/sessions")
    public Map<String, Object> listSessions(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "archived", required = false) Boolean archived) {
        return agentClient.listSessions(limit, archived);
    }

    @PostMapping("/sessions")
    public AgentSessionResponse createSession(@RequestBody Map<String, Object> request) {
        return agentClient.createSession(request);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String sessionId) {
        return agentClient.deleteSession(sessionId);
    }

    @PatchMapping("/sessions/{sessionId}")
    public Map<String, Object> updateSession(@PathVariable String sessionId, @RequestBody Map<String, Object> request) {
        return agentClient.updateSession(sessionId, request);
    }

    @PostMapping("/sessions/{sessionId}/share")
    public Map<String, Object> shareSession(@PathVariable String sessionId, @RequestBody Map<String, Object> request) {
        return agentClient.shareSession(sessionId, request);
    }

    @GetMapping("/shared/{shareId}")
    public Map<String, Object> getSharedSession(@PathVariable String shareId) {
        return agentClient.getSharedSession(shareId);
    }

    @PostMapping("/sessions/{sessionId}/runs")
    public AgentRunResponse runSession(@PathVariable String sessionId) {
        return agentClient.runSession(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public Map<String, Object> createMessage(@PathVariable String sessionId, @RequestBody Map<String, Object> request) {
        return agentClient.createMessage(sessionId, request);
    }

    @GetMapping(value = "/sessions/{sessionId}/runs/stream", produces = "text/event-stream")
    public ResponseEntity<StreamingResponseBody> streamRunSession(@PathVariable String sessionId) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(agentClient.streamRunSession(sessionId));
    }

    @GetMapping(value = "/sessions/{sessionId}/runs/{runId}/stream", produces = "text/event-stream")
    public ResponseEntity<StreamingResponseBody> streamRunSession(@PathVariable String sessionId, @PathVariable String runId) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(agentClient.streamRunSession(sessionId, runId));
    }

    @GetMapping("/sessions/{sessionId}/events")
    public Map<String, Object> listEvents(
            @PathVariable String sessionId,
            @RequestParam(value = "afterSeq", required = false) Long afterSeq) {
        return agentClient.listEvents(sessionId, afterSeq);
    }

    @GetMapping("/sessions/{sessionId}/runs/{runId}/events")
    public Map<String, Object> listRunEvents(
            @PathVariable String sessionId,
            @PathVariable String runId,
            @RequestParam(value = "afterSeq", required = false) Long afterSeq) {
        return agentClient.listRunEvents(sessionId, runId, afterSeq);
    }

    @GetMapping("/sessions/{sessionId}/timeline")
    public Map<String, Object> listTimeline(
            @PathVariable String sessionId,
            @RequestParam(value = "beforeCursor", required = false) String beforeCursor,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return agentClient.listTimeline(sessionId, beforeCursor, limit);
    }

    @GetMapping("/memories")
    public Map<String, Object> listMemories(@RequestParam(value = "status", required = false) String status) {
        return agentClient.listMemories(status);
    }

    @PostMapping("/memory-candidates/{candidateId}/accept")
    public Map<String, Object> acceptMemoryCandidate(@PathVariable String candidateId) {
        return agentClient.acceptMemoryCandidate(candidateId);
    }

    @PostMapping("/memory-candidates/{candidateId}/reject")
    public Map<String, Object> rejectMemoryCandidate(@PathVariable String candidateId) {
        return agentClient.rejectMemoryCandidate(candidateId);
    }

    @DeleteMapping("/memories/{memoryId}")
    public Map<String, Object> deleteMemory(@PathVariable String memoryId) {
        return agentClient.deleteMemory(memoryId);
    }

    @PostMapping("/exports")
    public Map<String, Object> createExport(@RequestBody Map<String, Object> request) {
        ExportFile file = exportService.createExport(request);
        return Map.of(
                "exportId", file.exportId(),
                "fileName", file.fileName(),
                "contentType", file.contentType(),
                "size", file.size(),
                "downloadUrl", "/manager/virtual-expert/agent/exports/" + file.exportId() + "/download"
        );
    }

    @GetMapping("/exports/{exportId}")
    public Map<String, Object> getExport(@PathVariable String exportId) {
        ExportFile file = exportService.findExport(exportId)
                .orElseThrow(() -> new IllegalArgumentException("Export file not found"));
        return Map.of(
                "exportId", file.exportId(),
                "fileName", file.fileName(),
                "contentType", file.contentType(),
                "size", file.size(),
                "downloadUrl", "/manager/virtual-expert/agent/exports/" + file.exportId() + "/download"
        );
    }

    @GetMapping("/exports/{exportId}/download")
    public ResponseEntity<Resource> downloadExport(@PathVariable String exportId) {
        ExportFile file = exportService.findExport(exportId)
                .orElseThrow(() -> new IllegalArgumentException("Export file not found"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName())
                        .build()
                        .toString())
                .body(new FileSystemResource(file.path()));
    }

    @PostMapping("/sessions/{sessionId}/runs/{runId}/cancel")
    public Map<String, Object> cancelRun(@PathVariable String sessionId, @PathVariable String runId) {
        return agentClient.cancelRun(sessionId, runId);
    }
}
