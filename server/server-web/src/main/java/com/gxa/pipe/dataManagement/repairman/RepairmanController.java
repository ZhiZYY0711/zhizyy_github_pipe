package com.gxa.pipe.dataManagement.repairman;

import com.gxa.pipe.entity.Repairman;
import com.gxa.pipe.utils.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/data_management/repairman")
@RequiredArgsConstructor
@Slf4j
public class RepairmanController {

    private final RepairmanService repairmanService;

    @PostMapping("/find_repairman_params")
    public Result<RepairmanPageQueryResponse> queryRepairmen(@Valid @RequestBody RepairmanQueryRequest request) {
        log.info("分页查询检修员，请求参数：{}", request);
        return Result.success(repairmanService.queryRepairmen(request));
    }

    @PostMapping("/find_repairman_id")
    public Result<RepairmanQueryResponse> getRepairmanById(@Valid @RequestBody RepairmanQueryByIdRequest request) {
        log.info("根据ID查询检修员，请求参数：{}", request);

        Repairman repairman = repairmanService.getById(Long.valueOf(request.getId()));
        if (repairman == null) {
            return Result.error("检修员不存在");
        }

        return Result.success(RepairmanServiceImpl.toQueryResponse(repairman));
    }

    @PostMapping("/add_repairman")
    public Result<Object> addRepairman(@Valid @RequestBody RepairmanAddRequest request) {
        log.info("新增检修员，请求参数：{}", request);

        boolean success = repairmanService.addRepairman(request);
        return success ? Result.success() : Result.error("新增检修员失败");
    }

    @PostMapping("/update_repairman")
    public Result<Object> updateRepairman(@Valid @RequestBody RepairmanUpdateRequest request) {
        log.info("修改检修员，请求参数：{}", request);

        boolean success = repairmanService.updateRepairman(request);
        return success ? Result.success() : Result.error("修改检修员失败");
    }

    @GetMapping("/delete_repairman/{ids}")
    public Result<Object> deleteRepairman(@PathVariable String ids) {
        log.info("删除检修员，请求参数：{}", ids);

        String[] idArray = ids.split(",");
        List<Long> idList = new ArrayList<>();
        for (String id : idArray) {
            idList.add(Long.valueOf(id.trim()));
        }

        boolean success = repairmanService.batchDeleteRepairmen(idList);
        return success ? Result.success() : Result.error("删除检修员失败");
    }

    @GetMapping("/Indicator_card")
    public Result<RepairmanIndicatorResponse> getRepairmanIndicator(@RequestParam(required = false) String area_id) {
        log.info("查询检修员指标，请求参数：{}", area_id);

        RepairmanIndicatorRequest request = new RepairmanIndicatorRequest();
        if (area_id != null && !area_id.isEmpty()) {
            request.setAreaId(Long.parseLong(area_id));
        }
        return Result.success(repairmanService.getRepairmanIndicator(request));
    }
}
