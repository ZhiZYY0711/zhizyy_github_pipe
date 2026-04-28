package com.gxa.pipe.pipelineTopology;

public interface PipelineTopologyService {
    MonitoringFilterOptionsResponse getMonitoringFilterOptions(
            String provinceCode,
            String cityCode,
            String districtCode);
}
