package com.gxa.pipe.dataVsualization.dashboard;

import com.gxa.pipe.dataVsualization.dataMonitoring.AreaDetailResponse;
import com.gxa.pipe.dataVsualization.total.RunningWaterResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DashboardMapper {

    DashboardCurrentSummaryRow selectCurrentSummary(@Param("areaId") Long areaId);

    List<AreaDetailResponse> selectMetricTrend(
            @Param("areaId") Long areaId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);

    Long selectWarningCount(
            @Param("areaId") Long areaId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);

    Long selectTodayWarningCount(@Param("areaId") Long areaId);

    AreaDetailResponse selectMetricAverage(
            @Param("areaId") Long areaId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);

    List<RunningWaterResponse> selectRecentAlarms(
            @Param("areaStart") Long areaStart,
            @Param("areaEnd") Long areaEnd,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("limit") int limit);

    DashboardFreshnessRow selectLatestFreshness();
}
