package com.gxa.pipe.controller.dataManagement;

import com.gxa.pipe.pojo.entity.Repairman;
import com.gxa.pipe.pojo.dto.request.RepairmanQueryRequest;
import com.gxa.pipe.service.RepairmanService;
import com.gxa.pipe.utils.Result;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 维修工控制器
 */
@RestController
@RequestMapping("/repairman")
@RequiredArgsConstructor
@Slf4j
public class RepairmanController {

    private final RepairmanService repairmanService;

    /**
     * 分页查询维修工
     */
    @GetMapping("/page")
    public Result<PageResult<Repairman>> getRepairmenByPage(@Valid RepairmanQueryRequest request) {
        log.info("分页查询维修工，请求参数：{}", request);

        try {
            PageResult<Repairman> result = repairmanService.getByPage(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询维修工失败：{}", e.getMessage());
            return Result.error("查询维修工失败");
        }
    }

    /**
     * 根据ID查询维修工
     */
    @GetMapping("/{id}")
    public Result<Repairman> getRepairmanById(@PathVariable Long id) {
        log.info("根据ID查询维修工，ID：{}", id);

        try {
            Repairman repairman = repairmanService.getById(id);
            if (repairman == null) {
                return Result.error("维修工不存在");
            }
            return Result.success(repairman);
        } catch (Exception e) {
            log.error("查询维修工失败：{}", e.getMessage());
            return Result.error("查询维修工失败");
        }
    }

    /**
     * 创建维修工
     */
    @PostMapping
    public Result<String> createRepairman(@Valid @RequestBody Repairman repairman) {
        log.info("创建维修工，维修工信息：{}", repairman);

        try {
            boolean success = repairmanService.create(repairman);
            if (success) {
                return Result.success("维修工创建成功");
            } else {
                return Result.error("维修工创建失败");
            }
        } catch (Exception e) {
            log.error("创建维修工失败：{}", e.getMessage());
            return Result.error("创建维修工失败");
        }
    }

    /**
     * 更新维修工
     */
    @PutMapping("/{id}")
    public Result<String> updateRepairman(@PathVariable Long id, @Valid @RequestBody Repairman repairman) {
        log.info("更新维修工，ID：{}，维修工信息：{}", id, repairman);

        try {
            repairman.setId(id);
            boolean success = repairmanService.update(repairman);
            if (success) {
                return Result.success("维修工更新成功");
            } else {
                return Result.error("维修工更新失败");
            }
        } catch (Exception e) {
            log.error("更新维修工失败：{}", e.getMessage());
            return Result.error("更新维修工失败");
        }
    }

    /**
     * 删除维修工
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteRepairman(@PathVariable Long id) {
        log.info("删除维修工，ID：{}", id);

        try {
            boolean success = repairmanService.delete(id);
            if (success) {
                return Result.success("维修工删除成功");
            } else {
                return Result.error("维修工删除失败");
            }
        } catch (Exception e) {
            log.error("删除维修工失败：{}", e.getMessage());
            return Result.error("删除维修工失败");
        }
    }
}