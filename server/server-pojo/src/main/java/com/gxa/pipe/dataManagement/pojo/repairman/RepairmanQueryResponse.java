package com.gxa.pipe.dataManagement.pojo.repairman;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检修员查询响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairmanQueryResponse {

    /**
     * 检修员的唯一标识
     */
    private String id;

    /**
     * 检修员姓名
     */
    private String name;

    /**
     * 年龄
     */
    private String age;

    /**
     * 性别（0 男 1 女）
     */
    private String sex;

    /**
     * 联系方式
     */
    private String phone;

    /**
     * 所属区域名称
     */
    @JsonProperty("area_name")
    private String areaName;

    /**
     * 入职时间
     */
    @JsonProperty("entry_time")
    private String entryTime;
}