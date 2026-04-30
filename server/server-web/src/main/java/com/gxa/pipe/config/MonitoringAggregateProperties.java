package com.gxa.pipe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "performance.monitoring-aggregate")
public class MonitoringAggregateProperties {

    private boolean enabled = false;
    private boolean refreshEnabled = false;
    private long todayRefreshIntervalMs = 600000L;
    private String repairCron = "0 20 3 * * *";
    private int repairWindowDays = 7;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    public void setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
    }

    public long getTodayRefreshIntervalMs() {
        return todayRefreshIntervalMs;
    }

    public void setTodayRefreshIntervalMs(long todayRefreshIntervalMs) {
        this.todayRefreshIntervalMs = todayRefreshIntervalMs;
    }

    public String getRepairCron() {
        return repairCron;
    }

    public void setRepairCron(String repairCron) {
        this.repairCron = repairCron;
    }

    public int getRepairWindowDays() {
        return repairWindowDays;
    }

    public void setRepairWindowDays(int repairWindowDays) {
        this.repairWindowDays = repairWindowDays;
    }
}
