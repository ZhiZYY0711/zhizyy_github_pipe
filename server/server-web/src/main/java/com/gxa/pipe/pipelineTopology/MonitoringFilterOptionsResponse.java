package com.gxa.pipe.pipelineTopology;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringFilterOptionsResponse {
    @JsonProperty("scope_level")
    private String scopeLevel;

    private List<MonitoringPipeOption> pipes = new ArrayList<>();
}
