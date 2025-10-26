package com.gxa.pipe.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.gxa.pipe.mapper.RepairmanMapper;
import com.gxa.pipe.pojo.entity.Repairman;
import com.gxa.pipe.pojo.dto.request.RepairmanQueryRequest;
import com.gxa.pipe.service.RepairmanService;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维修工服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepairmanServiceImpl implements RepairmanService {
    
    private final RepairmanMapper repairmanMapper;
    
    @Override
    public PageResult<Repairman> getByPage(RepairmanQueryRequest request) {
        log.info("分页查询维修工，请求参数：{}", request);
        
        // 设置分页参数
-        PageHelper.startPage(request.getPageNum(), request.getPageSize());
+        PageHelper.startPage(request.getPage(), request.getPageSize());
        
        // 查询数据
        Page<Repairman> page = (Page<Repairman>) repairmanMapper.selectByPage(request);
        
        // 构建分页结果
        PageResult<Repairman> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getResult());
-        result.setPageNum(request.getPageNum());
+        result.setPageNum(request.getPage());
        result.setPageSize(request.getPageSize());
        
        return result;
    }
    
    @Override
    public Repairman getById(Long id) {
        return repairmanMapper.selectById(id);
    }
    
    @Override
    public boolean create(Repairman repairman) {
        repairman.setCreateTime(LocalDateTime.now());
        repairman.setUpdateTime(LocalDateTime.now());
        
        return repairmanMapper.insert(repairman) > 0;
    }
    
    @Override
    public boolean update(Repairman repairman) {
        repairman.setUpdateTime(LocalDateTime.now());
        
        return repairmanMapper.update(repairman) > 0;
    }
    
    @Override
    public boolean delete(Long id) {
        return repairmanMapper.deleteById(id) > 0;
    }
    
    @Override
    public int countTotal() {
        return repairmanMapper.countTotal();
    }
    
    @Override
    public BigDecimal calculateAverageCompletionTime() {
-        return repairmanMapper.calculateAverageCompletionTime();
+        Double avg = repairmanMapper.calculateAverageCompletionTime();
+        return avg == null ? BigDecimal.ZERO : BigDecimal.valueOf(avg);
    }
}