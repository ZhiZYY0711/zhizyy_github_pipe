package com.gxa.pipe.dataManagement.pojo.repairman;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通过检修员id查询检修员详情请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairmanQueryByIdRequest {

    /**
     * 检修员的唯一标识
     */
    private String id;
}
