package com.gxa.pipe.dataVsualization.dataMonitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据监控服务实现类
 */
@Service
public class DataMonitoringServiceImpl implements DataMonitoringService {
    
    @Autowired
    private DataMonitoringMapper dataMonitoringMapper;
    
    @Override
    public List<AreaDetailResponse> getAreaDetails(AreaDetailRequest request) {
        // 参数校验
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        
        // 调用Mapper层查询数据
        return dataMonitoringMapper.selectAreaDetails(request);
    }
    
    @Override
    public List<PipeDetailResponse> getPipeDetails(PipeDetailRequest request) {
        // 参数校验
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        
        if (request.getId() == null) {
            throw new IllegalArgumentException("管道ID不能为空");
        }
        
        // 调用Mapper层查询数据
        return dataMonitoringMapper.selectPipeDetails(request);
    }
    
    @Override
    public PipeIndicatorResponse getPipeKeyIndicators(String pipeId) {
        // 调用Mapper层查询数据
        return dataMonitoringMapper.selectPipeKeyIndicators(pipeId);
    }
}