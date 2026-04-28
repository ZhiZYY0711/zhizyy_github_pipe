package com.gxa.pipe.pipelineTopology;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringFilterOptionsServiceTest {

    @Test
    void nationalScopeDisablesPipeFiltering() {
        PipelineTopologyMapper mapper = mock(PipelineTopologyMapper.class);
        PipelineTopologyServiceImpl service = new PipelineTopologyServiceImpl(mapper);

        MonitoringFilterOptionsResponse response = service.getMonitoringFilterOptions(null, null, null);

        assertThat(response.getScopeLevel()).isEqualTo("NATIONAL");
        assertThat(response.getPipes()).isEmpty();
    }

    @Test
    void provinceScopeDisablesPipeFiltering() {
        PipelineTopologyMapper mapper = mock(PipelineTopologyMapper.class);
        PipelineTopologyServiceImpl service = new PipelineTopologyServiceImpl(mapper);

        MonitoringFilterOptionsResponse response = service.getMonitoringFilterOptions("130000", null, null);

        assertThat(response.getScopeLevel()).isEqualTo("PROVINCE");
        assertThat(response.getPipes()).isEmpty();
    }

    @Test
    void cityScopeLoadsCityToCitySegmentsGroupedByPipe() {
        PipelineTopologyMapper mapper = mock(PipelineTopologyMapper.class);
        PipelineTopologyServiceImpl service = new PipelineTopologyServiceImpl(mapper);
        when(mapper.selectMonitoringFilterSegments(130100L, "CITY"))
                .thenReturn(List.of(new MonitoringFilterSegmentRow(
                        12L,
                        "京津主干线",
                        "TRUNK",
                        "CITY_TO_CITY",
                        301L,
                        "石家庄市-保定市段",
                        2,
                        130100L,
                        130600L,
                        "CITY_TO_CITY"
                )));

        MonitoringFilterOptionsResponse response = service.getMonitoringFilterOptions("130000", "130100", null);

        assertThat(response.getScopeLevel()).isEqualTo("CITY");
        assertThat(response.getPipes()).hasSize(1);
        assertThat(response.getPipes().get(0).getSegments()).hasSize(1);
        verify(mapper).selectMonitoringFilterSegments(130100L, "CITY");
    }

    @Test
    void districtScopeLoadsDistrictAndInDistrictSegments() {
        PipelineTopologyMapper mapper = mock(PipelineTopologyMapper.class);
        PipelineTopologyServiceImpl service = new PipelineTopologyServiceImpl(mapper);
        when(mapper.selectMonitoringFilterSegments(130102L, "DISTRICT")).thenReturn(Collections.emptyList());

        MonitoringFilterOptionsResponse response = service.getMonitoringFilterOptions("130000", "130100", "130102");

        assertThat(response.getScopeLevel()).isEqualTo("DISTRICT");
        verify(mapper).selectMonitoringFilterSegments(130102L, "DISTRICT");
    }
}
