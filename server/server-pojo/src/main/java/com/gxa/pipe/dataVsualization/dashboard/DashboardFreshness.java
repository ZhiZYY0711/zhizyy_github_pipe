package com.gxa.pipe.dataVsualization.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardFreshness {
    private String metricRefreshedAt;
    private String currentRefreshedAt;
    private String alarmRefreshedAt;
}
