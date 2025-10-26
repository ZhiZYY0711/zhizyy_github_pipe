package com.gxa.pipe.mapper;

import com.gxa.pipe.pojo.entity.Log;
import com.gxa.pipe.pojo.dto.request.LogQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 日志Mapper接口
 */
@Mapper
public interface LogMapper {
    
    /**
     * 分页查询日志
     * @param request 查询请求
     * @return 日志列表
     */
    List<Log> selectByPage(LogQueryRequest request);
    
    /**
     * 根据ID查询日志
     * @param id 日志ID
     * @return 日志信息
     */
    Log selectById(@Param("id") Long id);
    
    /**
     * 插入日志
     * @param log 日志信息
     * @return 影响行数
     */
    int insert(Log log);
    
    /**
     * 更新日志
     * @param log 日志信息
     * @return 影响行数
     */
    int update(Log log);
    
    /**
     * 删除日志
     * @param id 日志ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}