package com.gxa.pipe.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.gxa.pipe.pojo.vo.AreaResponse;

import java.util.List;

/**
 * 区域映射器
 */
@Mapper
public interface AreaMapper {

    /**
     * 查询所有省份
     * 
     * @return 省份列表
     */
    @Select("SELECT DISTINCT id as code, province as name FROM area WHERE district = '-1' AND city = '-1'")
    List<AreaResponse> selectProvinces();

    /**
     * 根据省份代码查询城市
     * 
     * @param provinceCode 省份代码
     * @return 城市列表
     */
    @Select("SELECT DISTINCT id as code, city as name FROM area WHERE province = (SELECT province FROM area WHERE id = #{provinceCode}) AND district = '-1' AND city != '-1'")
    List<AreaResponse> selectCitiesByProvinceCode(String provinceCode);

    /**
     * 根据城市代码查询区县
     * 
     * @param cityCode 城市代码
     * @return 区县列表
     */
    @Select("SELECT id as code, district as name FROM area WHERE province = (SELECT province FROM area WHERE id = #{cityCode}) AND city = (SELECT city FROM area WHERE id = #{cityCode}) AND district != '-1'")
    List<AreaResponse> selectDistrictsByCityCode(String cityCode);
}
