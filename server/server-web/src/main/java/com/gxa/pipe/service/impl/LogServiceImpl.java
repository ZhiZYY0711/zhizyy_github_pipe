package com.gxa.pipe.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.gxa.pipe.mapper.LogMapper;
import com.gxa.pipe.pojo.entity.Log;
import com.gxa.pipe.pojo.dto.request.LogQueryRequest;
import com.gxa.pipe.service.LogService;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 日志服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogServiceImpl implements LogService {
    
    private final LogMapper logMapper;
    
    @Override
    public PageResult<Log> getByPage(LogQueryRequest request) {
        log.info("分页查询日志，请求参数：{}", request);
        
        // 设置分页参数
        PageHelper.startPage(request.getPage(), request.getPageSize());
        
        // 查询数据
        Page<Log> page = (Page<Log>) logMapper.selectByPage(request);
        
        // 构建分页结果
        PageResult<Log> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getResult());
        result.setPageNum(request.getPage());
        result.setPageSize(request.getPageSize());
        
        return result;
    }
    
    @Override
    public Log getById(Long id) {
        return logMapper.selectById(id);
    }
    
    @Override
    public boolean create(Log log) {
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());
        
        return logMapper.insert(log) > 0;
    }
    
    @Override
    public boolean update(Log log) {
        log.setUpdateTime(LocalDateTime.now());
        
        return logMapper.update(log) > 0;
    }
    
    @Override
    public boolean delete(Long id) {
        return logMapper.deleteById(id) > 0;
    }
}