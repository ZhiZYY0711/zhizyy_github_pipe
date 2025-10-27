package com.gxa.pipe.pojo.vo.dataManagement.repairman;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检修员分页查询响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairmanPageQueryResponse {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 检修员列表
     */
    private List<RepairmanQueryResponse> records;
}