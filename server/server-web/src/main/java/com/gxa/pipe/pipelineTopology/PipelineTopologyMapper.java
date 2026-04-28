package com.gxa.pipe.pipelineTopology;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PipelineTopologyMapper {
    List<MonitoringFilterSegmentRow> selectMonitoringFilterSegments(
            @Param("areaId") Long areaId,
            @Param("scopeLevel") String scopeLevel);
}
