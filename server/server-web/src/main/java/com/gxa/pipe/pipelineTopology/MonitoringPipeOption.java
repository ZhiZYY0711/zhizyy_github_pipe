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
public class MonitoringPipeOption {
    private Long id;
    private String name;

    @JsonProperty("pipe_level")
    private String pipeLevel;

    @JsonProperty("segment_level")
    private String segmentLevel;

    private List<MonitoringSegmentOption> segments = new ArrayList<>();
}
