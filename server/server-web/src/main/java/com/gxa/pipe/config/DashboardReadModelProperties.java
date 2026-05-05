package com.gxa.pipe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "performance.dashboard-read-model")
public class DashboardReadModelProperties {

    private boolean refreshEnabled = true;
    private boolean refreshOnStartup = true;
    private long refreshIntervalMs = 600000L;
    private long initialDelayMs = 30000L;
    private int alarmLimit = 5000;

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    public void setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
    }

    public boolean isRefreshOnStartup() {
        return refreshOnStartup;
    }

    public void setRefreshOnStartup(boolean refreshOnStartup) {
        this.refreshOnStartup = refreshOnStartup;
    }

    public long getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    public void setRefreshIntervalMs(long refreshIntervalMs) {
        this.refreshIntervalMs = refreshIntervalMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public int getAlarmLimit() {
        return alarmLimit;
    }

    public void setAlarmLimit(int alarmLimit) {
        this.alarmLimit = alarmLimit;
    }
}
