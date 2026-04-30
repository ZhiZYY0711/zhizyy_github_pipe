package com.gxa.pipe.dataVsualization.dataMonitoring;

import com.gxa.pipe.config.MonitoringAggregateProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 数据监控服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataMonitoringServiceImpl implements DataMonitoringService {
    
    private final DataMonitoringMapper dataMonitoringMapper;
    private final MonitoringAggregateProperties monitoringAggregateProperties;
    
    @Override
    public List<AreaDetailResponse> getAreaDetails(AreaDetailRequest request) {
        // 参数校验
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        
        if (monitoringAggregateProperties.isEnabled()) {
            try {
                List<AreaDetailResponse> aggregateRows =
                        dataMonitoringMapper.selectAreaDetailsFromDailyAggregate(request);
                if (aggregateRows != null && !aggregateRows.isEmpty()) {
                    return aggregateRows;
                }
            } catch (RuntimeException exception) {
                log.warn("监测区域聚合查询失败，回退原始明细聚合", exception);
            }
        }

        return dataMonitoringMapper.selectAreaDetails(request);
    }
    
    @Override
    public List<PipeDetailResponse> getPipeDetails(PipeDetailRequest request) {
        // 参数校验
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        List<Long> segmentIds = request.getSegmentIds();
        if (request.getSegmentId() != null && segmentIds != null && !segmentIds.isEmpty()) {
            throw new IllegalArgumentException("segment_id 和 segment_ids 不能同时存在");
        }
        
        if (request.getId() == null && request.getSegmentId() == null && (segmentIds == null || segmentIds.isEmpty())) {
            throw new IllegalArgumentException("管道ID或管段ID不能为空");
        }

        validateContinuousSegments(request.getId(), segmentIds);
        
        if (monitoringAggregateProperties.isEnabled()) {
            try {
                List<PipeDetailResponse> aggregateRows =
                        dataMonitoringMapper.selectPipeDetailsFromDailyAggregate(request);
                if (aggregateRows != null && !aggregateRows.isEmpty()) {
                    return aggregateRows;
                }
            } catch (RuntimeException exception) {
                log.warn("监测管道聚合查询失败，回退原始明细聚合", exception);
            }
        }

        return dataMonitoringMapper.selectPipeDetails(request);
    }
    
    @Override
    public PipeIndicatorResponse getPipeKeyIndicators(String pipeId, String segmentId, String segmentIdsValue) {
        List<Long> segmentIds = parseSegmentIds(segmentIdsValue);
        if (StringUtils.hasText(segmentId) && !segmentIds.isEmpty()) {
            throw new IllegalArgumentException("segment_id 和 segment_ids 不能同时存在");
        }

        Long pipeIdValue = StringUtils.hasText(pipeId) ? Long.parseLong(pipeId) : null;
        validateContinuousSegments(pipeIdValue, segmentIds);

        // 调用Mapper层查询数据
        return dataMonitoringMapper.selectPipeKeyIndicators(pipeId, segmentId, segmentIds);
    }

    private List<Long> parseSegmentIds(String segmentIdsValue) {
        if (!StringUtils.hasText(segmentIdsValue)) {
            return List.of();
        }

        String[] parts = segmentIdsValue.split(",");
        List<Long> segmentIds = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                segmentIds.add(Long.parseLong(part.trim()));
            }
        }
        return segmentIds;
    }

    private void validateContinuousSegments(Long pipeId, List<Long> segmentIds) {
        if (segmentIds == null || segmentIds.isEmpty()) {
            return;
        }

        if (pipeId == null) {
            throw new IllegalArgumentException("多管段查询必须传入管道ID");
        }

        List<PipeSegmentSelection> selections = dataMonitoringMapper.selectPipeSegmentSelections(pipeId, segmentIds);
        if (selections.size() != segmentIds.size()) {
            throw new IllegalArgumentException("管段不属于所选管道");
        }

        selections.sort(Comparator.comparing(PipeSegmentSelection::getSegmentOrder));
        for (int index = 0; index < selections.size(); index++) {
            PipeSegmentSelection selection = selections.get(index);
            if (!pipeId.equals(selection.getPipeId())) {
                throw new IllegalArgumentException("管段不属于所选管道");
            }

            if (index > 0) {
                int previousOrder = selections.get(index - 1).getSegmentOrder();
                if (selection.getSegmentOrder() != previousOrder + 1) {
                    throw new IllegalArgumentException("请选择连续管段");
                }
            }
        }
    }
}
