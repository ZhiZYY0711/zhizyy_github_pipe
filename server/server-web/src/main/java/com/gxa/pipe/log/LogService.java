package com.gxa.pipe.log;

import com.gxa.pipe.utils.PageResult;

/**
 * 日志服务接口
 */
public interface LogService {

    /**
     * 分页查询日志
     * 
     * @param request 查询请求参数
     * @return 分页结果
     */
    PageResult<LogQueryResponse> queryLogs(LogQueryRequest request);

    /**
     * 根据ID查询日志详细信息
     * 
     * @param id 日志ID
     * @return 日志详细信息
     */
    LogQueryResponse getLogById(Long id);

    /**
     * 获取日志指标卡
     * 
     * @param areaId 区域ID（可选）
     * @return 日志指标卡数据
     */
    LogIndicardResponse getLogIndicatorCard(Long areaId);
}