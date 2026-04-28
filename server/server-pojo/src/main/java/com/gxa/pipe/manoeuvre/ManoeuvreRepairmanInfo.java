package com.gxa.pipe.manoeuvre;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 演习检修员关联信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManoeuvreRepairmanInfo {

    /**
     * 演习ID
     */
    private Long manoeuvreId;

    /**
     * 检修员ID
     */
    private Long repairmanId;

    /**
     * 检修员姓名
     */
    private String repairmanName;
}