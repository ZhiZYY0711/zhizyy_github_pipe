package com.gxa.pipe.dataVsualization.dashboard;

import org.springframework.stereotype.Component;

@Component
public class AreaScopeResolver {

    private static final long COUNTRY_CODE = 100000L;

    public AreaScope resolve(Long areaId) {
        if (areaId == null || areaId == 0L || areaId.equals(COUNTRY_CODE)) {
            return new AreaScope(areaId, COUNTRY_CODE, AreaScope.Level.COUNTRY, 100000L, 999999L);
        }

        if (areaId % 10000 == 0) {
            return new AreaScope(areaId, areaId, AreaScope.Level.PROVINCE, areaId, areaId + 9999L);
        }

        if (areaId % 100 == 0) {
            return new AreaScope(areaId, areaId, AreaScope.Level.CITY, areaId, areaId + 99L);
        }

        return new AreaScope(areaId, areaId, AreaScope.Level.DISTRICT, areaId, areaId);
    }
}
