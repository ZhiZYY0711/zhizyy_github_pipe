package com.gxa.pipe.log;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 日志Mapper接口
 */
@Mapper
public interface LogMapper {

    /**
     * 根据条件分页查询日志信息
     * 
     * @param request 查询请求参数
     * @return 日志查询响应列表
     */
    List<LogQueryResponse> selectLogsByParams(LogQueryRequest request);

    /**
     * 根据条件统计日志总数
     * 
     * @param request 查询请求参数
     * @return 日志总数
     */
    Long countLogsByParams(LogQueryRequest request);

    /**
     * 根据用户ID和类型查询用户名
     * 
     * @param userId 用户ID
     * @param type   用户类型 0-管理员 1-检修员
     * @return 用户名
     */
    String selectUsernameByUserIdAndType(@Param("userId") Long userId, @Param("type") Integer type);

    /**
     * 根据日志ID查询日志详细信息
     * 
     * @param id 日志ID
     * @return 日志详细信息
     */
    LogQueryResponse selectLogById(@Param("id") Long id);

    /**
     * 统计全部日志数量
     * 
     * @param areaId 区域ID（可选）
     * @return 全部日志数量
     */
    Long countAllLogs(@Param("areaId") Long areaId);

    /**
     * 统计警告日志数量（状态为2）
     * 
     * @param areaId 区域ID（可选）
     * @return 警告日志数量
     */
    Long countWarningLogs(@Param("areaId") Long areaId);

    /**
     * 统计错误日志数量（状态为1）
     * 
     * @param areaId 区域ID（可选）
     * @return 错误日志数量
     */
    Long countErrorLogs(@Param("areaId") Long areaId);

    /**
     * 统计调试日志数量（状态为3）
     * 
     * @param areaId 区域ID（可选）
     * @return 调试日志数量
     */
    Long countDebuggingLogs(@Param("areaId") Long areaId);

    /**
     * 统计今日日志数量
     * 
     * @param areaId 区域ID（可选）
     * @return 今日日志数量
     */
    Long countTodayLogs(@Param("areaId") Long areaId);
}