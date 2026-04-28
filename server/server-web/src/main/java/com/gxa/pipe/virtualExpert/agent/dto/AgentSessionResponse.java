package com.gxa.pipe.virtualExpert.agent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AgentSessionResponse(
        @JsonAlias("session_id") String sessionId,
        String status
) {
}
