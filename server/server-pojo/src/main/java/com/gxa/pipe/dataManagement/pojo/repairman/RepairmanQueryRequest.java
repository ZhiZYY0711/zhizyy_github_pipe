package com.gxa.pipe.dataManagement.pojo.repairman;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * 检修员查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairmanQueryRequest {

    /**
     * 页码，从1开始
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page;

    /**
     * 每页数量
     */
    @JsonProperty("page_size")
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize;

    /**
     * 检修员姓名
     */
    private String name;

    /**
     * 性别（0 男 1 女 2 未知）
     */
    private String sex;

    /**
     * 最小年龄
     */
    @JsonProperty("min_age")
    private String minAge;

    /**
     * 最大年龄
     */
    @JsonProperty("max_age")
    private String maxAge;

    /**
     * 所属区域id
     */
    @JsonProperty("area_id")
    private String areaId;

    /**
     * 入职起始时间
     */
    @JsonProperty("entry_start_time")
    private String entryStartTime;

    /**
     * 入职结束时间
     */
    @JsonProperty("entry_end_time")
    private String entryEndTime;

    /**
     * 联系电话
     */
    private String phone;
}