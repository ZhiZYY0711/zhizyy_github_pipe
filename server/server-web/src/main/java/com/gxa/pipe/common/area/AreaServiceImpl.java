package com.gxa.pipe.common.area;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaServiceImpl implements AreaService {

    private final AreaMapper areaMapper;

    @Override
    public List<AreaResponse> getProvinces() {
        return areaMapper.selectProvinces();
    }

    @Override
    public List<AreaResponse> getCitiesByProvinceCode(String provinceCode) {
        return areaMapper.selectCitiesByProvinceCode(provinceCode);
    }

    @Override
    public List<AreaResponse> getDistrictsByCityCode(String cityCode) {
        return areaMapper.selectDistrictsByCityCode(cityCode);
    }
}
