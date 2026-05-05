package com.gxa.pipe.dataVsualization.dashboard;

public record AreaScope(
        Long requestedAreaId,
        Long rollupAreaId,
        Level level,
        Long areaStart,
        Long areaEnd) {

    public enum Level {
        COUNTRY,
        PROVINCE,
        CITY,
        DISTRICT
    }
}
