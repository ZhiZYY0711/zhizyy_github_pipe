package com.gxa.pipe.virtualExpert.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record ToolQueryResponse(
        String metric,
        String window,
        List<Map<String, Object>> points,
        String summary,
        Map<String, Object> context,
        List<Map<String, Object>> records,
        List<Map<String, Object>> facts,
        Double confidence,
        Map<String, Object> metadata
) {
    @JsonProperty("raw_ref")
    public Map<String, Object> rawRef() {
        return Map.of("source", "server-web");
    }
}
