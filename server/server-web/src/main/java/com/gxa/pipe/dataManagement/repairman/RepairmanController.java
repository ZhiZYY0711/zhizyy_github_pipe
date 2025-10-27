package com.gxa.pipe.dataManagement.repairman;

import com.gxa.pipe.entity.Repairman;
import com.gxa.pipe.utils.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 检修员控制器
 */
@RestController
@RequestMapping("/data_management/repairman")
@RequiredArgsConstructor
@Slf4j
public class RepairmanController {

    private final RepairmanService repairmanService;

    /**
     * 查询检修员（非id）
     */
    @PostMapping("/find_repairman_params")
    public Result<List<RepairmanQueryResponse>> queryRepairmen(@Valid @RequestBody RepairmanQueryRequest request) {
        log.info("分页查询检修员，请求参数：{}", request);

        try {
            RepairmanPageQueryResponse result = repairmanService.queryRepairmen(request);
            // 根据接口文档，应返回数组格式
            return Result.success(result.getRecords());
        } catch (Exception e) {
            log.error("分页查询检修员失败：{}", e.getMessage());
            return Result.error("查询检修员失败");
        }
    }

    /**
     * 查询检修员（按id）
     */
    @PostMapping("/find_repairman_id")
    public Result<RepairmanQueryResponse> getRepairmanById(@Valid @RequestBody RepairmanQueryByIdRequest request) {
        log.info("根据ID查询检修员，请求参数：{}", request);

        try {
            Repairman repairman = repairmanService.getById(Long.valueOf(request.getId()));
            if (repairman != null) {
                // 转换为响应格式
                RepairmanQueryResponse response = new RepairmanQueryResponse();
                response.setId(String.valueOf(repairman.getId()));
                response.setName(repairman.getName());
                response.setAge(repairman.getAge() != null ? String.valueOf(repairman.getAge()) : null);
                response.setSex(repairman.getSex() != null ? String.valueOf(repairman.getSex()) : null);
                response.setPhone(repairman.getPhone());
                response.setAreaName(""); // 暂时设为空字符串，需要从区域表查询
                response.setEntryTime(repairman.getEntryTime() != null ? repairman.getEntryTime().toString() : null);
                return Result.success(response);
            } else {
                return Result.error("检修员不存在");
            }
        } catch (Exception e) {
            log.error("根据ID查询检修员失败：{}", e.getMessage());
            return Result.error("查询检修员失败");
        }
    }

    /**
     * 新增检修员
     */
    @PostMapping("/add_repairman")
    public Result<Object> addRepairman(@Valid @RequestBody RepairmanAddRequest request) {
        log.info("新增检修员，请求参数：{}", request);

        try {
            boolean success = repairmanService.addRepairman(request);
            if (success) {
                return Result.success();
            } else {
                return Result.error("新增检修员失败");
            }
        } catch (Exception e) {
            log.error("新增检修员失败：{}", e.getMessage());
            return Result.error("新增检修员失败");
        }
    }

    /**
     * 更新检修员信息
     */
    @PostMapping("/update_repairman")
    public Result<Object> updateRepairman(@Valid @RequestBody RepairmanUpdateRequest request) {
        log.info("修改检修员，请求参数：{}", request);

        try {
            boolean success = repairmanService.updateRepairman(request);
            if (success) {
                return Result.success(); // 返回成功响应，不带数据
            } else {
                return Result.error("修改检修员失败");
            }
        } catch (Exception e) {
            log.error("修改检修员失败：{}", e.getMessage());
            return Result.error("修改检修员失败");
        }
    }

    /**
     * 删除检修员（按ids）
     */
    @GetMapping("/delete_repairman/{ids}")
    public Result<Object> deleteRepairman(@PathVariable String ids) {
        log.info("删除检修员，请求参数：{}", ids);

        try {
            // 解析逗号分隔的ID字符串
            String[] idArray = ids.split(",");
            List<Long> idList = new ArrayList<>();
            for (String id : idArray) {
                idList.add(Long.valueOf(id.trim()));
            }

            boolean success = repairmanService.batchDeleteRepairmen(idList);
            if (success) {
                return Result.success(); // 返回成功响应，不带数据
            } else {
                return Result.error("删除检修员失败");
            }
        } catch (Exception e) {
            log.error("删除检修员失败：{}", e.getMessage());
            return Result.error("删除检修员失败");
        }
    }

    /**
     * 获取检修员指标卡数据
     */
    @GetMapping("/Indicator_card")
    public Result<RepairmanIndicatorResponse> getRepairmanIndicator(@RequestParam(required = false) String area_id) {
        log.info("查询检修员指标，请求参数：{}", area_id);

        try {
            RepairmanIndicatorRequest request = new RepairmanIndicatorRequest();
            if (area_id != null && !area_id.isEmpty()) {
                request.setAreaId(Long.parseLong(area_id));
            }
            RepairmanIndicatorResponse result = repairmanService.getRepairmanIndicator(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询检修员指标失败：{}", e.getMessage());
            return Result.error("查询检修员指标失败");
        }
    }
}