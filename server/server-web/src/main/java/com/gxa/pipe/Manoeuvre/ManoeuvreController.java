package com.gxa.pipe.manoeuvre;

import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 演习控制器
 */
@RestController
@RequestMapping("/manoeuvre")
@RequiredArgsConstructor
@Slf4j
public class ManoeuvreController {

  private final ManoeuvreService manoeuvreService;

  /**
   * 查询演练信息（非id）
   */
  @PostMapping("/find_manoeuvre_params")
  public Result<List<ManoeuvreQueryResponse>> findManoeuvreByParams(@Valid @RequestBody ManoeuvreQueryRequest request) {
    log.info("查询演练信息，请求参数：{}", request);

    try {
      List<ManoeuvreQueryResponse> result = manoeuvreService.findManoeuvreByParams(request);
      return Result.success(result);
    } catch (Exception e) {
      log.error("查询演练信息失败：{}", e.getMessage(), e);
      return Result.error("查询演练信息失败");
    }
  }

  /**
   * 查询演练信息（id）
   */
  @GetMapping("/find_manoeuvre_id")
  public Result<List<ManoeuvreQueryResponse>> findManoeuvreById(@RequestParam("id") Long id) {
    log.info("根据ID查询演练信息，演习ID：{}", id);

    try {
      List<ManoeuvreQueryResponse> result = manoeuvreService.findManoeuvreById(id);
      return Result.success(result);
    } catch (Exception e) {
      log.error("根据ID查询演练信息失败：{}", e.getMessage(), e);
      return Result.error("根据ID查询演练信息失败");
    }
  }

  /**
   * 添加演练信息
   */
  @PostMapping("/add_manoeuvre")
  public Result<Void> addManoeuvre(@Valid @RequestBody ManoeuvreAddRequest request) {
    log.info("添加演练信息，请求参数：{}", request);

    try {
      manoeuvreService.addManoeuvre(request);
      return Result.success();
    } catch (Exception e) {
      log.error("添加演练信息失败：{}", e.getMessage(), e);
      return Result.error("添加演练信息失败");
    }
  }

  /**
   * 修改演练信息
   */
  @PostMapping("/update_manoeuvre")
  public Result<Void> updateManoeuvre(@Valid @RequestBody ManoeuvreUpdateRequest request) {
    log.info("修改演练信息，请求参数：{}", request);

    try {
      manoeuvreService.updateManoeuvre(request);
      return Result.success();
    } catch (Exception e) {
      log.error("修改演练信息失败：{}", e.getMessage(), e);
      return Result.error("修改演练信息失败");
    }
  }

  /**
   * 删除演练信息
   */
  @GetMapping("/remove_manoeuvre")
  public Result<Void> removeManoeuvre(@RequestParam("id") Long id) {
    log.info("删除演练信息，演习ID：{}", id);

    try {
      manoeuvreService.removeManoeuvre(id);
      return Result.success();
    } catch (Exception e) {
      log.error("删除演练信息失败：{}", e.getMessage(), e);
      return Result.error("删除演练信息失败");
    }
  }
}
