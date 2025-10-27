package com.gxa.pipe.pojo.dto.dataManagement.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监测数据查询请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataQueryRequest {

    /**
     * 最小压力
     */
    @JsonProperty("min_pressure")
    private String minPressure;

    /**
     * 最大压力
     */
    @JsonProperty("max_pressure")
    private String maxPressure;

    /**
     * 最小流量
     */
    @JsonProperty("min_flow")
    private String minFlow;

    /**
     * 最大流量
     */
    @JsonProperty("max_flow")
    private String maxFlow;

    /**
     * 最小温度
     */
    @JsonProperty("min_temperature")
    private String minTemperature;

    /**
     * 最大温度
     */
    @JsonProperty("max_temperature")
    private String maxTemperature;

    /**
     * 最小震动
     */
    @JsonProperty("min_vibration")
    private String minVibration;

    /**
     * 最大震动
     */
    @JsonProperty("max_vibration")
    private String maxVibration;

    /**
     * 数据状态（0 安全 1 良好 2 危险 3 高危）
     */
    @JsonProperty("data_status")
    private String dataStatus;

    /**
     * 传感器id
     */
    @JsonProperty("sensor_id")
    private String sensorId;

    /**
     * 管道名称
     */
    @JsonProperty("pipeline_name")
    private String pipelineName;

    /**
     * 监测起始时间
     */
    @JsonProperty("monitor_start_time")
    private String monitorStartTime;

    /**
     * 监测结束时间
     */
    @JsonProperty("monitor_end_time")
    private String monitorEndTime;

    /**
     * 页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    @JsonProperty("page_size")
    private Integer pageSize;
}
