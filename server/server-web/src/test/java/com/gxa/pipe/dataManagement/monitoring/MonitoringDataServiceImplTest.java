package com.gxa.pipe.dataManagement.monitoring;

import com.gxa.pipe.config.MonitoringAggregateProperties;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringDataServiceImplTest {

    @Test
    void blankRawFilterUsesRawCount() {
        MonitoringDataMapper mapper = mock(MonitoringDataMapper.class);
        MonitoringAggregateProperties properties = new MonitoringAggregateProperties();
        properties.setEnabled(true);
        MonitoringDataServiceImpl service = new MonitoringDataServiceImpl(mapper, properties);
        MonitoringDataQueryRequest request = new MonitoringDataQueryRequest();
        request.setMaxPressure("");

        when(mapper.countByConditions(request)).thenReturn(0L);

        service.getByPage(request);

        verify(mapper).countByConditions(request);
        verify(mapper, never()).countByDailySummary(null);
        verify(mapper, never()).countByDailyAggregate(request);
    }

    @Test
    void blankStatusUsesRawCount() {
        MonitoringDataMapper mapper = mock(MonitoringDataMapper.class);
        MonitoringAggregateProperties properties = new MonitoringAggregateProperties();
        properties.setEnabled(true);
        MonitoringDataServiceImpl service = new MonitoringDataServiceImpl(mapper, properties);
        MonitoringDataQueryRequest request = new MonitoringDataQueryRequest();
        request.setDataStatus("");

        when(mapper.countByConditions(request)).thenReturn(0L);

        service.getByPage(request);

        verify(mapper).countByConditions(request);
        verify(mapper, never()).countByDailySummary(null);
        verify(mapper, never()).countByDailyAggregate(request);
    }
}
