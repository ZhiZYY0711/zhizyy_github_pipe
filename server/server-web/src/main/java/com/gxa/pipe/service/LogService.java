package com.gxa.pipe.service;

import com.gxa.pipe.pojo.dto.LogQueryRequest;
import com.gxa.pipe.pojo.entity.Log;
import com.gxa.pipe.utils.PageResult;

/**
 * 日志服务接口
 */
public interface LogService {

    /**
     * 分页查询日志
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<Log> getByPage(LogQueryRequest request);

    /**
     * 根据ID查询日志
     * 
     * @param id 日志ID
     * @return 日志信息
     */
    Log getById(Long id);

    /**
     * 创建日志
     * 
     * @param log 日志信息
     * @return 是否成功
     */
    boolean create(Log log);

    /**
     * 更新日志
     * 
     * @param log 日志信息
     * @return 是否成功
     */
    boolean update(Log log);

    /**
     * 删除日志
     * 
     * @param id 日志ID
     * @return 是否成功
     */
    boolean delete(Long id);
}