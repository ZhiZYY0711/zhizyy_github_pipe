package com.gxa.pipe.mapper;

import com.gxa.pipe.pojo.entity.MonitoringData;
import com.gxa.pipe.pojo.dto.request.MonitoringDataQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 监测数据Mapper接口
 */
@Mapper
public interface MonitoringDataMapper {
    
    /**
     * 分页查询监测数据
     * @param request 查询请求
     * @return 监测数据列表
     */
    List<MonitoringData> selectByPage(MonitoringDataQueryRequest request);
    
    /**
     * 根据ID查询监测数据
     * @param id 监测数据ID
     * @return 监测数据
     */
    MonitoringData selectById(@Param("id") Long id);
    
    /**
     * 插入监测数据
     * @param monitoringData 监测数据
     * @return 影响行数
     */
    int insert(MonitoringData monitoringData);
    
    /**
     * 更新监测数据
     * @param monitoringData 监测数据
     * @return 影响行数
     */
    int update(MonitoringData monitoringData);
    
    /**
     * 删除监测数据
     * @param id 监测数据ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
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