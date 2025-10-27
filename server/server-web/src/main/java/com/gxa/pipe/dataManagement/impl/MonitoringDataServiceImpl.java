package com.gxa.pipe.dataManagement.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.gxa.pipe.dataManagement.mapper.MonitoringDataMapper;
import com.gxa.pipe.dataManagement.service.MonitoringDataService;
import com.gxa.pipe.pojo.dto.dataManagement.monitoring.*;
import com.gxa.pipe.pojo.vo.dataManagement.monitoring.*;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 监测数据服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringDataServiceImpl implements MonitoringDataService {

    private final MonitoringDataMapper monitoringDataMapper;

    @Override
    public PageResult<MonitoringDataQueryResponse> getByPage(MonitoringDataQueryRequest request) {
        log.info("分页查询监测数据，参数：{}", request);

        // 设置分页参数
        PageHelper.startPage(request.getPage(), request.getPageSize());

        // 查询数据
        Page<MonitoringDataQueryResponse> page = monitoringDataMapper.getByPage(request);

        return new PageResult<>(page.getTotal(), page.getResult());
    }

    @Override
    public List<MonitoringDataQueryResponse> getById(MonitoringDataByIdRequest request) {
        log.info("通过ID查询监测数据，参数：{}", request);
        return monitoringDataMapper.getById(request);
    }

    @Override
    public void addData(MonitoringDataAddRequest request) {
        log.info("添加监测数据，参数：{}", request);
        monitoringDataMapper.addData(request);
    }

    @Override
    public void updateData(MonitoringDataUpdateRequest request) {
        log.info("修改监测数据，参数：{}", request);
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
        return monitoringDataMapper.getIndicatorCard(areaId);
    }
}