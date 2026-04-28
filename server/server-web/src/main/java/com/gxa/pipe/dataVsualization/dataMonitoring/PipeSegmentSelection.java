package com.gxa.pipe.dataVsualization.dataMonitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipeSegmentSelection {
    private Long id;
    private Long pipeId;
    private Integer segmentOrder;
}
