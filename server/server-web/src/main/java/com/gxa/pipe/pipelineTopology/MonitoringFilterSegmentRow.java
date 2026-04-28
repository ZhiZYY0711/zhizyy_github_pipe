package com.gxa.pipe.pipelineTopology;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringFilterSegmentRow {
    private Long pipeId;
    private String pipeName;
    private String pipeLevel;
    private String pipeSegmentLevel;
    private Long segmentId;
    private String segmentName;
    private Integer segmentOrder;
    private Long startAreaId;
    private Long endAreaId;
    private String segmentLevel;
}
