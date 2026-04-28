package com.gxa.pipe.virtualExpert.agent;

import java.util.Map;

class AgentServiceException extends RuntimeException {
    private final int statusCode;
    private final Map<String, Object> body;

    AgentServiceException(int statusCode, Map<String, Object> body) {
        super(String.valueOf(body.getOrDefault("message", "Agent request failed")));
        this.statusCode = statusCode;
        this.body = body;
    }

    int statusCode() {
        return statusCode;
    }

    Map<String, Object> body() {
        return body;
    }
}
