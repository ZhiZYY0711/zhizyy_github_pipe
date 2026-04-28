package com.gxa.pipe.manoeuvre;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

/**
 * 演习查询响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManoeuvreQueryResponse {

    /**
     * 演习ID
     */
    private Long id;

    /**
     * 演习名称
     */
    private String name;

    /**
     * 区域ID
     */
    private Long areaId;

    /**
     * 区域名称
     */
    private String areaName;

    /**
     * 演习地点
     */
    private String location;

    /**
     * 开始时间（毫秒时间戳）
     */
    private Long startTime;

    /**
     * 结束时间（毫秒时间戳）
     */
    private Long endTime;

    /**
     * 组织单位
     */
    private String organizer;

    /**
     * 状态：0 成功 1 失败 2 进行中
     */
    private Integer status;

    /**
     * 类型：0 状态模拟 1 事故模拟 2 紧急事故 3 日常作训
     */
    private Integer type;

    /**
     * 演习描述
     */
    private String details;

    /**
     * 参加演习的检修员列表
     */
    private List<HashMap<String, Object>> repairmans;
}
