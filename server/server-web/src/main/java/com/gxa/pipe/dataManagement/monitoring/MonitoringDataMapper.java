package com.gxa.pipe.dataManagement.monitoring;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MonitoringDataMapper {

    List<MonitoringDataQueryResponse> getByPage(
            @Param("request") MonitoringDataQueryRequest request,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    Long countByConditions(MonitoringDataQueryRequest request);

    Map<String, Object> aggregateByConditions(MonitoringDataQueryRequest request);

    List<MonitoringDataQueryResponse> getById(MonitoringDataByIdRequest request);

    void addData(MonitoringDataAddRequest request);

    void updateData(MonitoringDataUpdateRequest request);

    void deleteData(@Param("idList") List<String> idList);

    MonitoringDataIndicatorResponse getIndicatorCard(@Param("areaId") String areaId);

    MonitoringDataIndicatorResponse getIndicatorCardFromDailyAggregate(@Param("areaId") String areaId);
}
