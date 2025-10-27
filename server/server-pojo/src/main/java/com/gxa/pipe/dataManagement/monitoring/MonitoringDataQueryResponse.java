package com.gxa.pipe.dataManagement.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监测数据查询响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataQueryResponse {

    /**
     * 监测数据的唯一标识
     */
    private String id;

    /**
     * 压力
     */
    private String pressure;

    /**
     * 流量
     */
    private String flow;

    /**
     * 温度
     */
    private String temperature;

    /**
     * 震动
     */
    private String vibration;

    /**
     * 数据状态
     */
    @JsonProperty("data_status")
    private String dataStatus;

    /**
     * 传感器ID
     */
    @JsonProperty("sensor_id")
    private String sensorId;

    /**
     * 区域名称
     */
    @JsonProperty("area_name")
    private String areaName;

    /**
     * 管道名称
     */
    @JsonProperty("pipeline_name")
    private String pipelineName;

    /**
     * 监测时间
     */
    @JsonProperty("monitor_time")
    private String monitorTime;
}
