package com.gxa.pipe.pipelineTopology;

import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/pipeline_topology")
@RequiredArgsConstructor
public class PipelineTopologyController {
    private final PipelineTopologyService pipelineTopologyService;

    @GetMapping("/monitoring_filter_options")
    public Result<MonitoringFilterOptionsResponse> getMonitoringFilterOptions(
            @RequestParam(value = "province_code", required = false) String provinceCode,
            @RequestParam(value = "city_code", required = false) String cityCode,
            @RequestParam(value = "district_code", required = false) String districtCode) {
        log.info("获取监测页管网筛选选项，province={}, city={}, district={}", provinceCode, cityCode, districtCode);
        return Result.success(pipelineTopologyService.getMonitoringFilterOptions(provinceCode, cityCode, districtCode));
    }
}
