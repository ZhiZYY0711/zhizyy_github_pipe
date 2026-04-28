package com.gxa.pipe.virtualExpert.agent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;
import java.util.Map;

public record AgentRunResponse(
        @JsonAlias("session_id") String sessionId,
        @JsonAlias("run_id") String runId,
        List<Map<String, Object>> events
) {
}
