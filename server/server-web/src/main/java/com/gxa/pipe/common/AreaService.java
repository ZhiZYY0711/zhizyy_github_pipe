package com.gxa.pipe.common;

import java.util.List;

public interface AreaService {

    /**
     * 获取所有省份信息
     * 
     * @return 省份列表
     */
    List<AreaResponse> getProvinces();

    /**
     * 根据省份代码获取城市信息
     * 
     * @param provinceCode 省份代码
     * @return 城市列表
     */
    List<AreaResponse> getCitiesByProvinceCode(String provinceCode);

    /**
     * 根据城市代码获取区县信息
     * 
     * @param cityCode 城市代码
     * @return 区县列表
     */
    List<AreaResponse> getDistrictsByCityCode(String cityCode);
}
