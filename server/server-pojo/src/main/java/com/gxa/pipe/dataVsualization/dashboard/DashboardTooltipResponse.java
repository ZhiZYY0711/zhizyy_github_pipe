package com.gxa.pipe.dataVsualization.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardTooltipResponse {
    private String areaId;
    private String areaName;
    private double flow;
    private double pressure;
    private double temperature;
    private double vibration;
    private long sensorNumbers;
    private long abnormalSensorNumbers;
    private long warnings;
}
