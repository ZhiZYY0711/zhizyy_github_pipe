package com.gxa.pipe.pipelineTopology;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PipelineTopologyServiceImpl implements PipelineTopologyService {
    private final PipelineTopologyMapper pipelineTopologyMapper;

    @Override
    public MonitoringFilterOptionsResponse getMonitoringFilterOptions(
            String provinceCode,
            String cityCode,
            String districtCode) {
        String scopeLevel = resolveScopeLevel(provinceCode, cityCode, districtCode);
        if ("NATIONAL".equals(scopeLevel) || "PROVINCE".equals(scopeLevel)) {
            return new MonitoringFilterOptionsResponse(scopeLevel, List.of());
        }

        Long areaId = Long.parseLong("DISTRICT".equals(scopeLevel) ? districtCode : cityCode);
        return new MonitoringFilterOptionsResponse(
                scopeLevel,
                groupRows(pipelineTopologyMapper.selectMonitoringFilterSegments(areaId, scopeLevel)));
    }

    private String resolveScopeLevel(String provinceCode, String cityCode, String districtCode) {
        if (StringUtils.hasText(districtCode)) {
            return "DISTRICT";
        }
        if (StringUtils.hasText(cityCode)) {
            return "CITY";
        }
        if (StringUtils.hasText(provinceCode)) {
            return "PROVINCE";
        }
        return "NATIONAL";
    }

    private List<MonitoringPipeOption> groupRows(List<MonitoringFilterSegmentRow> rows) {
        Map<Long, MonitoringPipeOption> pipeMap = new LinkedHashMap<>();
        for (MonitoringFilterSegmentRow row : rows) {
            MonitoringPipeOption pipe = pipeMap.computeIfAbsent(row.getPipeId(), pipeId ->
                    new MonitoringPipeOption(
                            pipeId,
                            row.getPipeName(),
                            row.getPipeLevel(),
                            row.getPipeSegmentLevel(),
                            new ArrayList<>()));

            pipe.getSegments().add(new MonitoringSegmentOption(
                    row.getSegmentId(),
                    row.getSegmentName(),
                    row.getSegmentOrder(),
                    row.getStartAreaId(),
                    row.getEndAreaId(),
                    row.getSegmentLevel()));
        }
        return new ArrayList<>(pipeMap.values());
    }
}
