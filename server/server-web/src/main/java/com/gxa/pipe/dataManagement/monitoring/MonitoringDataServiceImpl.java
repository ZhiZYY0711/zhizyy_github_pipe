package com.gxa.pipe.dataManagement.monitoring;

import com.gxa.pipe.config.MonitoringAggregateProperties;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        Long total = monitoringDataMapper.countByConditions(request);
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
                        monitoringDataMapper.getIndicatorCardFromDailyAggregate(areaId);
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
