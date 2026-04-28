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
     * 数据状态文本
     */
    @JsonProperty("status_text")
    private String statusText;

    /**
     * 传感器ID
     */
    @JsonProperty("sensor_id")
    private String sensorId;

    /**
     * 传感器名称
     */
    @JsonProperty("sensor_name")
    private String sensorName;

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
     * 管段ID
     */
    @JsonProperty("pipe_segment_id")
    private String pipeSegmentId;

    /**
     * 管段名称
     */
    @JsonProperty("pipe_segment_name")
    private String pipeSegmentName;

    /**
     * 监测时间
     */
    @JsonProperty("monitor_time")
    private String monitorTime;
}
