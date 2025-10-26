package com.gxa.pipe.service.dataManagement;

import com.gxa.pipe.pojo.dto.request.dataManagement.monitoring.MonitoringDataAddRequest;
import com.gxa.pipe.pojo.dto.request.dataManagement.monitoring.MonitoringDataByIdRequest;
import com.gxa.pipe.pojo.dto.request.dataManagement.monitoring.MonitoringDataQueryRequest;
import com.gxa.pipe.pojo.dto.request.dataManagement.monitoring.MonitoringDataUpdateRequest;
import com.gxa.pipe.pojo.dto.response.dataManagement.monitoring.MonitoringDataIndicatorResponse;
import com.gxa.pipe.pojo.dto.response.dataManagement.monitoring.MonitoringDataQueryResponse;
import com.gxa.pipe.utils.PageResult;

import java.util.List;

/**
 * 监测数据服务接口
 */
public interface MonitoringDataService {

    /**
     * 分页查询监测数据
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<MonitoringDataQueryResponse> getByPage(MonitoringDataQueryRequest request);

    /**
     * 通过ID查询监测数据
     * 
     * @param request 查询请求
     * @return 查询结果
     */
    List<MonitoringDataQueryResponse> getById(MonitoringDataByIdRequest request);

    /**
     * 添加监测数据
     * 
     * @param request 添加请求
     */
    void addData(MonitoringDataAddRequest request);

    /**
     * 修改监测数据
     * 
     * @param request 修改请求
     */
    void updateData(MonitoringDataUpdateRequest request);

    /**
     * 删除监测数据
     * 
     * @param ids 删除的监测数据的id数组
     */
    void deleteData(String ids);

    /**
     * 获取监测数据指标卡
     * 
     * @param areaId 区域id（可选）
     * @return 指标卡数据
     */
    MonitoringDataIndicatorResponse getIndicatorCard(String areaId);
}