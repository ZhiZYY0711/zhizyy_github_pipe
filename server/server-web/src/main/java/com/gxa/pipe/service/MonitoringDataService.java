package com.gxa.pipe.service;

import com.gxa.pipe.pojo.entity.MonitoringData;
import com.gxa.pipe.pojo.dto.request.MonitoringDataQueryRequest;
import com.gxa.pipe.utils.PageResult;

/**
 * 监测数据服务接口
 */
public interface MonitoringDataService {
    
    /**
     * 分页查询监测数据
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<MonitoringData> getByPage(MonitoringDataQueryRequest request);
    
    /**
     * 根据ID查询监测数据
     * @param id 监测数据ID
     * @return 监测数据信息
     */
    MonitoringData getById(Long id);
    
    /**
     * 创建监测数据
     * @param monitoringData 监测数据信息
     * @return 是否成功
     */
    boolean create(MonitoringData monitoringData);
    
    /**
     * 更新监测数据
     * @param monitoringData 监测数据信息
     * @return 是否成功
     */
    boolean update(MonitoringData monitoringData);
    
    /**
     * 删除监测数据
     * @param id 监测数据ID
     * @return 是否成功
     */
    boolean delete(Long id);
    
    /**
     * 统计异常传感器数量
     * @return 异常传感器数量
     */
    int countAbnormalSensors();
    
    /**
     * 统计传感器总数
     * @return 传感器总数
     */
    int countTotalSensors();
}