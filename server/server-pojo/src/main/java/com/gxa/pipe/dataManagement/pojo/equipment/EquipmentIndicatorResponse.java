package com.gxa.pipe.dataManagement.pojo.equipment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备指标卡响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentIndicatorResponse {

    /**
     * 设备总数
     */
    private String total;

    /**
     * 正常设备数
     */
    private String normal;

    /**
     * 故障设备数
     */
    private String fault;

    /**
     * 维护中设备数
     */
    private String maintenance;

    /**
     * 离线设备数
     */
    private String offline;
}