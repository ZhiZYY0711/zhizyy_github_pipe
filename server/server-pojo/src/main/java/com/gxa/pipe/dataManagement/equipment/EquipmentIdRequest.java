package com.gxa.pipe.dataManagement.equipment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备ID查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentIdRequest {

    /**
     * 设备的唯一标识
     */
    private String id;
}