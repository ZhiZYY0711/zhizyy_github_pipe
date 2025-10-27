package com.gxa.pipe.common;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.gxa.pipe.utils.Result;

import lombok.RequiredArgsConstructor;

/**
 * 区域控制器
 */
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/area_details")
@RequiredArgsConstructor
public class AreaController {
  private final AreaService areaService;

  /**
   * 获取省份信息
   * 
   * @return 省份列表
   */
  @GetMapping("/provinces")
  public Result<List<AreaResponse>> getProvinces() {
    log.info("获取省份信息");
    List<AreaResponse> provinces = areaService.getProvinces();
    return Result.success(provinces);
  }

  /**
   * 根据省份代码获取城市信息
   * 
   * @param provinceCode 省份代码
   * @return 城市列表
   */
  @GetMapping("/citys/{province_code}")
  public Result<List<AreaResponse>> getCitiesByProvinceCode(@PathVariable("province_code") String provinceCode) {
    log.info("根据省份代码获取城市信息，省份代码：{}", provinceCode);
    List<AreaResponse> cities = areaService.getCitiesByProvinceCode(provinceCode);
    return Result.success(cities);
  }

  /**
   * 根据城市代码获取区县信息
   * 
   * @param cityCode 城市代码
   * @return 区县列表
   */
  @GetMapping("/districts/{city_code}")
  public Result<List<AreaResponse>> getDistrictsByCityCode(@PathVariable("city_code") String cityCode) {
    log.info("根据城市代码获取区县信息，城市代码：{}", cityCode);
    List<AreaResponse> districts = areaService.getDistrictsByCityCode(cityCode);
    return Result.success(districts);
  }
}
