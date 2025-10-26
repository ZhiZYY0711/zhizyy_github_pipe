package com.gxa.pipe.service.impl;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.gxa.pipe.service.AreaService;
import com.gxa.pipe.mapper.AreaMapper;
import com.gxa.pipe.pojo.dto.response.AreaResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaServiceImple implements AreaService {

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
