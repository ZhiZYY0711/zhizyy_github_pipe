package com.gxa.pipe.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页数据
     */
    private List<T> records;
    
    /**
     * 当前页码
     */
    private Integer pageNum;
    
    /**
     * 每页大小
     */
    private Integer pageSize;
    
    /**
     * 总页数
     */
    private Integer pages;
    
    /**
     * 构造方法
     */
    public PageResult(Long total, List<T> records) {
        this.total = total;
        this.records = records;
    }
    
    /**
     * 计算总页数
     */
    public Integer getPages() {
        if (pageSize == null || pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
}