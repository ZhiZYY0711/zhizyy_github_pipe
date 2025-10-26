package com.gxa.pipe.pojo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检修员查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairmanQueryRequest {
    
    /**
     * 页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer pageSize;
    
    /**
     * 姓名
     */
    private String name;
    
    /**
     * 年龄
     */
    private Integer age;
    
    /**
     * 性别
     */
    private Integer sex;
    
    /**
     * 电话
     */
    private String phone;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 区域ID
     */
    private Long areaId;
}