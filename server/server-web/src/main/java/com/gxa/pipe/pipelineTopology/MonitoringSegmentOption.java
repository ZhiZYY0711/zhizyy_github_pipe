package com.gxa.pipe.pipelineTopology;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringSegmentOption {
    private Long id;
    private String name;

    @JsonProperty("segment_order")
    private Integer segmentOrder;

    @JsonProperty("start_area_id")
    private Long startAreaId;

    @JsonProperty("end_area_id")
    private Long endAreaId;

    @JsonProperty("segment_level")
    private String segmentLevel;
}
