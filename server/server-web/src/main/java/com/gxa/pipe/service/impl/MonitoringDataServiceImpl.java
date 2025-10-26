package com.gxa.pipe.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.gxa.pipe.mapper.MonitoringDataMapper;
import com.gxa.pipe.pojo.entity.MonitoringData;
import com.gxa.pipe.pojo.dto.request.MonitoringDataQueryRequest;
import com.gxa.pipe.service.MonitoringDataService;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    public PageResult<MonitoringData> getByPage(MonitoringDataQueryRequest request) {
        log.info("分页查询监测数据，请求参数：{}", request);
        
        // 设置分页参数
        PageHelper.startPage(request.getPage(), request.getPageSize());
        
        // 查询数据
        Page<MonitoringData> page = (Page<MonitoringData>) monitoringDataMapper.selectByPage(request);
        
        // 构建分页结果
        PageResult<MonitoringData> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getResult());
        result.setPageNum(request.getPage());
        result.setPageSize(request.getPageSize());
        
        return result;
    }
    
    @Override
    public MonitoringData getById(Long id) {
        return monitoringDataMapper.selectById(id);
    }
    
    @Override
    public boolean create(MonitoringData monitoringData) {
        monitoringData.setCreateTime(LocalDateTime.now());
        monitoringData.setUpdateTime(LocalDateTime.now());
        
        return monitoringDataMapper.insert(monitoringData) > 0;
    }
    
    @Override
    public boolean update(MonitoringData monitoringData) {
        monitoringData.setUpdateTime(LocalDateTime.now());
        
        return monitoringDataMapper.update(monitoringData) > 0;
    }
    
    @Override
    public boolean delete(Long id) {
        return monitoringDataMapper.deleteById(id) > 0;
    }
    
    @Override
    public int countAbnormalSensors() {
        return monitoringDataMapper.countAbnormalSensors();
    }
    
    @Override
    public int countTotalSensors() {
        return monitoringDataMapper.countTotalSensors();
    }
}