package com.gxa.pipe.mapper;

import com.github.pagehelper.Page;
import com.gxa.pipe.pojo.dto.request.MonitoringDataAddRequest;
import com.gxa.pipe.pojo.dto.request.MonitoringDataByIdRequest;
import com.gxa.pipe.pojo.dto.request.MonitoringDataQueryRequest;
import com.gxa.pipe.pojo.dto.request.MonitoringDataUpdateRequest;
import com.gxa.pipe.pojo.dto.response.MonitoringDataIndicatorResponse;
import com.gxa.pipe.pojo.dto.response.MonitoringDataQueryResponse;
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
     * @return 分页结果
     */
    Page<MonitoringDataQueryResponse> getByPage(MonitoringDataQueryRequest request);
    
    /**
     * 通过ID查询监测数据
     * @param request 查询请求
     * @return 查询结果
     */
    List<MonitoringDataQueryResponse> getById(MonitoringDataByIdRequest request);
    
    /**
     * 添加监测数据
     * @param request 添加请求
     */
    void addData(MonitoringDataAddRequest request);
    
    /**
     * 修改监测数据
     * @param request 修改请求
     */
    void updateData(MonitoringDataUpdateRequest request);
    
    /**
     * 删除监测数据
     * @param idList 删除的监测数据的id列表
     */
    void deleteData(@Param("idList") List<String> idList);
    
    /**
     * 获取监测数据指标卡
     * @param areaId 区域id（可选）
     * @return 指标卡数据
     */
    MonitoringDataIndicatorResponse getIndicatorCard(@Param("areaId") String areaId);
}