package com.gxa.pipe.dataManagement.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监测数据指标卡响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataIndicatorResponse {

    /**
     * 总计数据
     */
    private String total;

    /**
     * 正常数据
     */
    private String normal;

    /**
     * 异常数据
     */
    private String abnormal;

    /**
     * 今日新增
     */
    private String today;
}