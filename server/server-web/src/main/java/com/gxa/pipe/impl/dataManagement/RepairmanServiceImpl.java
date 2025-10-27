package com.gxa.pipe.impl.dataManagement;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.gxa.pipe.mapper.dataManagement.RepairmanMapper;
import com.gxa.pipe.pojo.dto.dataManagement.repairman.*;
import com.gxa.pipe.pojo.vo.dataManagement.repairman.*;
import com.gxa.pipe.pojo.entity.Repairman;
import com.gxa.pipe.service.dataManagement.RepairmanService;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 检修员服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepairmanServiceImpl implements RepairmanService {

    private final RepairmanMapper repairmanMapper;

    @Override
    public RepairmanPageQueryResponse queryRepairmen(RepairmanQueryRequest request) {
        log.info("分页查询检修员，请求参数：{}", request);

        // 设置分页参数
        PageHelper.startPage(request.getPage(), request.getPageSize());

        // 查询数据
        Page<Repairman> page = (Page<Repairman>) repairmanMapper.selectByPage(request);

        // 转换为响应格式
        List<RepairmanQueryResponse> dataList = new ArrayList<>();
        for (Repairman repairman : page.getResult()) {
            RepairmanQueryResponse item = new RepairmanQueryResponse();
            item.setId(String.valueOf(repairman.getId()));
            item.setName(repairman.getName());
            item.setAge(repairman.getAge() != null ? String.valueOf(repairman.getAge()) : null);
            item.setSex(repairman.getSex() != null ? String.valueOf(repairman.getSex()) : null);
            item.setPhone(repairman.getPhone());
            item.setAreaName(""); // 暂时设为空字符串，需要从区域表查询
            item.setEntryTime(repairman.getEntryTime() != null ? repairman.getEntryTime().toString() : null);
            dataList.add(item);
        }

        // 构建响应
        RepairmanPageQueryResponse response = new RepairmanPageQueryResponse();
        response.setTotal(page.getTotal());
        response.setRecords(dataList);

        return response;
    }

    @Override
    @Deprecated
    public PageResult<Repairman> getByPage(RepairmanQueryRequest request) {
        log.info("分页查询检修员（兼容性方法），请求参数：{}", request);

        // 设置分页参数
        PageHelper.startPage(request.getPage(), request.getPageSize());

        // 查询数据
        Page<Repairman> page = (Page<Repairman>) repairmanMapper.selectByPage(request);

        // 构建分页结果
        PageResult<Repairman> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getResult());
        result.setPageNum(request.getPage());
        result.setPageSize(request.getPageSize());

        return result;
    }

    @Override
    public Repairman getById(Long id) {
        return repairmanMapper.selectById(id);
    }

    @Override
    public boolean addRepairman(RepairmanAddRequest request) {
        log.info("新增检修员，请求参数：{}", request);

        Repairman repairman = new Repairman();
        BeanUtils.copyProperties(request, repairman);
        repairman.setCreateTime(LocalDateTime.now());
        repairman.setUpdateTime(LocalDateTime.now());

        return repairmanMapper.insert(repairman) > 0;
    }

    @Override
    public boolean updateRepairman(RepairmanUpdateRequest request) {
        log.info("修改检修员，请求参数：{}", request);

        Repairman repairman = new Repairman();
        BeanUtils.copyProperties(request, repairman);
        repairman.setUpdateTime(LocalDateTime.now());

        return repairmanMapper.update(repairman) > 0;
    }

    @Override
    public boolean deleteRepairman(Long id) {
        log.info("删除检修员，ID：{}", id);
        return repairmanMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean batchDeleteRepairmen(List<Long> ids) {
        log.info("批量删除检修员，IDs：{}", ids);

        if (ids == null || ids.isEmpty()) {
            return false;
        }

        return repairmanMapper.batchDeleteByIds(ids) > 0;
    }

    @Override
    public RepairmanIndicatorResponse getRepairmanIndicator(RepairmanIndicatorRequest request) {
        log.info("查询检修员指标，请求参数：{}", request);

        RepairmanIndicatorResponse response = new RepairmanIndicatorResponse();

        // 查询总数
        int totalCount = repairmanMapper.countTotal();
        response.setTotal(String.valueOf(totalCount));

        // 查询男性数量
        int maleCount = repairmanMapper.countBySex("0");
        response.setMale(String.valueOf(maleCount));

        // 查询女性数量
        int femaleCount = repairmanMapper.countBySex("1");
        response.setFemale(String.valueOf(femaleCount));

        // 查询平均年龄
        Double avgAge = repairmanMapper.calculateAverageAge();
        response.setAvgAge(String.valueOf(avgAge != null ? avgAge : 0.0));

        // 设置本月新增人数（暂时设为0，需要根据实际需求实现）
        response.setNewThisMonth("0");

        return response;
    }

    @Override
    @Deprecated
    public boolean create(Repairman repairman) {
        repairman.setCreateTime(LocalDateTime.now());
        repairman.setUpdateTime(LocalDateTime.now());

        return repairmanMapper.insert(repairman) > 0;
    }

    @Override
    @Deprecated
    public boolean update(Repairman repairman) {
        repairman.setUpdateTime(LocalDateTime.now());
        return repairmanMapper.update(repairman) > 0;
    }

    @Override
    @Deprecated
    public boolean delete(Long id) {
        return repairmanMapper.deleteById(id) > 0;
    }

    @Override
    public int countTotal() {
        return repairmanMapper.countTotal();
    }

    @Override
    public BigDecimal calculateAverageCompletionTime() {
        Double avg = repairmanMapper.calculateAverageCompletionTime();
        return avg == null ? BigDecimal.ZERO : BigDecimal.valueOf(avg);
    }
}