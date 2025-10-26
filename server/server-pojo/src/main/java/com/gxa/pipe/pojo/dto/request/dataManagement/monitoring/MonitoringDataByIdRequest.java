package com.gxa.pipe.pojo.dto.request.dataManagement.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通过ID查询监测数据请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataByIdRequest {

    /**
     * 监测数据的唯一标识
     */
    private String id;
}