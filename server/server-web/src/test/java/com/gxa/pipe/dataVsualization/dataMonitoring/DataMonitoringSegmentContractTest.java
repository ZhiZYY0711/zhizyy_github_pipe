package com.gxa.pipe.dataVsualization.dataMonitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataMonitoringSegmentContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void pipeDetailsRequestAcceptsSegmentIds() throws Exception {
        PipeDetailRequest request = objectMapper.readValue("""
                {
                  "id": 12,
                  "segment_ids": [301, 302]
                }
                """, PipeDetailRequest.class);

        assertThat(request.getId()).isEqualTo(12L);
        assertThat(request.getSegmentIds()).containsExactly(301L, 302L);
    }

    @Test
    void pipeDetailsCanQueryBySegmentIdWithoutPipeId() {
        DataMonitoringMapper mapper = mock(DataMonitoringMapper.class);
        DataMonitoringServiceImpl service = new DataMonitoringServiceImpl(mapper);
        PipeDetailRequest request = new PipeDetailRequest();
        request.setSegmentId(9L);
        when(mapper.selectPipeDetails(request)).thenReturn(Collections.emptyList());

        service.getPipeDetails(request);

        verify(mapper).selectPipeDetails(request);
    }

    @Test
    void pipeDetailsRequirePipeIdOrSegmentId() {
        DataMonitoringMapper mapper = mock(DataMonitoringMapper.class);
        DataMonitoringServiceImpl service = new DataMonitoringServiceImpl(mapper);

        assertThatThrownBy(() -> service.getPipeDetails(new PipeDetailRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("管道ID或管段ID不能为空");
    }

    @Test
    void pipeDetailsRejectsSegmentIdAndSegmentIdsTogether() {
        DataMonitoringMapper mapper = mock(DataMonitoringMapper.class);
        DataMonitoringServiceImpl service = new DataMonitoringServiceImpl(mapper);
        PipeDetailRequest request = new PipeDetailRequest();
        request.setId(12L);
        request.setSegmentId(301L);
        request.setSegmentIds(List.of(301L, 302L));

        assertThatThrownBy(() -> service.getPipeDetails(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("segment_id 和 segment_ids 不能同时存在");
    }

    @Test
    void pipeDetailsAcceptsContinuousSegmentIds() {
        DataMonitoringMapper mapper = mock(DataMonitoringMapper.class);
        DataMonitoringServiceImpl service = new DataMonitoringServiceImpl(mapper);
        PipeDetailRequest request = new PipeDetailRequest();
        request.setId(12L);
        request.setSegmentIds(List.of(301L, 302L, 303L));
        when(mapper.selectPipeSegmentSelections(12L, request.getSegmentIds())).thenReturn(Arrays.asList(
                new PipeSegmentSelection(301L, 12L, 2),
                new PipeSegmentSelection(302L, 12L, 3),
                new PipeSegmentSelection(303L, 12L, 4)
        ));
        when(mapper.selectPipeDetails(request)).thenReturn(Collections.emptyList());

        service.getPipeDetails(request);

        verify(mapper).selectPipeDetails(request);
    }

    @Test
    void pipeDetailsRejectsDiscontinuousSegmentIds() {
        DataMonitoringMapper mapper = mock(DataMonitoringMapper.class);
        DataMonitoringServiceImpl service = new DataMonitoringServiceImpl(mapper);
        PipeDetailRequest request = new PipeDetailRequest();
        request.setId(12L);
        request.setSegmentIds(List.of(301L, 303L));
        when(mapper.selectPipeSegmentSelections(12L, request.getSegmentIds())).thenReturn(Arrays.asList(
                new PipeSegmentSelection(301L, 12L, 2),
                new PipeSegmentSelection(303L, 12L, 4)
        ));

        assertThatThrownBy(() -> service.getPipeDetails(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请选择连续管段");
    }

    @Test
    void keyIndicatorsAcceptsContinuousSegmentIds() {
        DataMonitoringMapper mapper = mock(DataMonitoringMapper.class);
        DataMonitoringServiceImpl service = new DataMonitoringServiceImpl(mapper);
        when(mapper.selectPipeSegmentSelections(12L, List.of(301L, 302L))).thenReturn(Arrays.asList(
                new PipeSegmentSelection(301L, 12L, 2),
                new PipeSegmentSelection(302L, 12L, 3)
        ));
        when(mapper.selectPipeKeyIndicators("12", null, List.of(301L, 302L))).thenReturn(new PipeIndicatorResponse());

        service.getPipeKeyIndicators("12", null, "301,302");

        verify(mapper).selectPipeKeyIndicators("12", null, List.of(301L, 302L));
    }
}
