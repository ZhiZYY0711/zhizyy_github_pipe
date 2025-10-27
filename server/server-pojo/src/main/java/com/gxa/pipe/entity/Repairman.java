package com.gxa.pipe.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 检修员表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repairman {

    /**
     * 检修员的唯一标识
     */
    private Long id;

    /**
     * 姓名
     */
    private String name;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别 0 男 1 女 2 未知
     */
    private Integer sex;

    /**
     * 联系方式
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 入职时间
     */
    private LocalDateTime entryTime;

    /**
     * 所属区域
     */
    private Long areaId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}