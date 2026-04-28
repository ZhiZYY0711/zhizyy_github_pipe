package com.gxa.pipe.manoeuvre;

import com.gxa.pipe.utils.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manoeuvre")
@RequiredArgsConstructor
@Slf4j
public class ManoeuvreController {

  private final ManoeuvreService manoeuvreService;

  @PostMapping("/find_manoeuvre_params")
  public Result<List<ManoeuvreQueryResponse>> findManoeuvreByParams(
      @Valid @RequestBody ManoeuvreQueryRequest request) {
    log.info("查询演练信息，请求参数：{}", request);
    return Result.success(manoeuvreService.findManoeuvreByParams(request));
  }

  @GetMapping("/find_manoeuvre_id")
  public Result<List<ManoeuvreQueryResponse>> findManoeuvreById(@RequestParam("id") Long id) {
    log.info("根据ID查询演练信息，演练ID：{}", id);
    return Result.success(manoeuvreService.findManoeuvreById(id));
  }

  @PostMapping("/add_manoeuvre")
  public Result<Void> addManoeuvre(@Valid @RequestBody ManoeuvreAddRequest request) {
    log.info("添加演练信息，请求参数：{}", request);
    manoeuvreService.addManoeuvre(request);
    return Result.success();
  }

  @PostMapping("/update_manoeuvre")
  public Result<Void> updateManoeuvre(@Valid @RequestBody ManoeuvreUpdateRequest request) {
    log.info("修改演练信息，请求参数：{}", request);
    manoeuvreService.updateManoeuvre(request);
    return Result.success();
  }

  @GetMapping("/remove_manoeuvre")
  public Result<Void> removeManoeuvre(@RequestParam("id") Long id) {
    log.info("删除演练信息，演练ID：{}", id);
    manoeuvreService.removeManoeuvre(id);
    return Result.success();
  }
}
