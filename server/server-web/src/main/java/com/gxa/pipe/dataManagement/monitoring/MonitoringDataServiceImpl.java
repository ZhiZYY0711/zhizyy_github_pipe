package com.gxa.pipe.dataManagement.monitoring;

import com.gxa.pipe.config.MonitoringAggregateProperties;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringDataServiceImpl implements MonitoringDataService {

    private final MonitoringDataMapper monitoringDataMapper;
    private final MonitoringAggregateProperties monitoringAggregateProperties;

    @Override
    public PageResult<MonitoringDataQueryResponse> getByPage(MonitoringDataQueryRequest request) {
        log.info("分页查询监测数据，请求参数：{}", request);

        int pageNum = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 50;
        int offset = (pageNum - 1) * pageSize;

        Long total = countByFastPath(request);
        List<MonitoringDataQueryResponse> records =
                total != null && total > 0
                        ? monitoringDataMapper.getByPage(request, offset, pageSize)
                        : Collections.emptyList();

        PageResult<MonitoringDataQueryResponse> result = new PageResult<>();
        result.setTotal(total != null ? total : 0L);
        result.setRecords(records);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    private Long countByFastPath(MonitoringDataQueryRequest request) {
        if (!canUseAggregateCount(request)) {
            return monitoringDataMapper.countByConditions(request);
        }

        try {
            if (isGlobalAggregateCount(request)) {
                return monitoringDataMapper.countByDailySummary(normalizeDataStatus(request.getDataStatus()));
            }

            return monitoringDataMapper.countByDailyAggregate(request);
        } catch (RuntimeException exception) {
            log.warn("监测数据聚合计数失败，回退明细计数", exception);
            return monitoringDataMapper.countByConditions(request);
        }
    }

    private boolean canUseAggregateCount(MonitoringDataQueryRequest request) {
        if (!monitoringAggregateProperties.isEnabled()) {
            return false;
        }

        return request.getMinPressure() == null
                && request.getMaxPressure() == null
                && request.getMinFlow() == null
                && request.getMaxFlow() == null
                && request.getMinTemperature() == null
                && request.getMaxTemperature() == null
                && request.getMinVibration() == null
                && request.getMaxVibration() == null
                && request.getSensorId() == null
                && isNullOrEmpty(request.getPipelineName())
                && request.getMonitorStartTime() == null
                && request.getMonitorEndTime() == null
                && isSupportedDataStatus(request.getDataStatus());
    }

    private boolean isGlobalAggregateCount(MonitoringDataQueryRequest request) {
        return isNullOrEmpty(request.getAreaId())
                && isNullOrEmpty(request.getPipelineId())
                && isNullOrEmpty(request.getPipeSegmentId())
                && (request.getPipeSegmentIds() == null || request.getPipeSegmentIds().isEmpty());
    }

    private boolean isSupportedDataStatus(String dataStatus) {
        return dataStatus == null
                || "0".equals(dataStatus)
                || "1".equals(dataStatus)
                || "2".equals(dataStatus)
                || "3".equals(dataStatus);
    }

    private String normalizeDataStatus(String dataStatus) {
        return dataStatus;
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    @Override
    public Map<String, Object> aggregateByConditions(MonitoringDataQueryRequest request) {
        log.info("聚合查询监测数据，请求参数：{}", request);
        Map<String, Object> aggregate = monitoringDataMapper.aggregateByConditions(request);
        return aggregate == null ? Collections.emptyMap() : aggregate;
    }

    @Override
    public List<MonitoringDataQueryResponse> getById(MonitoringDataByIdRequest request) {
        log.info("通过ID查询监测数据，请求参数：{}", request);
        return monitoringDataMapper.getById(request);
    }

    @Override
    public void addData(MonitoringDataAddRequest request) {
        log.info("添加监测数据，请求参数：{}", request);
        monitoringDataMapper.addData(request);
    }

    @Override
    public void updateData(MonitoringDataUpdateRequest request) {
        log.info("修改监测数据，请求参数：{}", request);
        monitoringDataMapper.updateData(request);
    }

    @Override
    public void deleteData(String ids) {
        log.info("删除监测数据，ids：{}", ids);
        List<String> idList = Arrays.asList(ids.split(","));
        monitoringDataMapper.deleteData(idList);
    }

    @Override
    public MonitoringDataIndicatorResponse getIndicatorCard(String areaId) {
        log.info("获取监测数据指标卡，areaId：{}", areaId);
        if (monitoringAggregateProperties.isEnabled()) {
            try {
                MonitoringDataIndicatorResponse aggregate =
                        StringUtils.hasText(areaId)
                                ? monitoringDataMapper.getIndicatorCardFromDailyAggregate(areaId)
                                : monitoringDataMapper.getIndicatorCardFromDailySummary();
                if (aggregate != null) {
                    return aggregate;
                }
            } catch (RuntimeException exception) {
                log.warn("监测指标卡聚合查询失败，回退原始明细聚合", exception);
            }
        }

        return monitoringDataMapper.getIndicatorCard(areaId);
    }
}
