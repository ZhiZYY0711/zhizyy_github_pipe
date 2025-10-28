package com.gxa.pipe.manoeuvre;

import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 演习服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManoeuvreServiceImpl implements ManoeuvreService {

    private final ManoeuvreMapper manoeuvreMapper;

    @Override
    public List<ManoeuvreQueryResponse> findManoeuvreByParams(ManoeuvreQueryRequest request) {
        log.info("根据条件查询演习信息，请求参数：{}", request);

        // 设置分页参数
        PageHelper.startPage(request.getPage(), request.getPageSize());

        // 查询基础演习信息
        List<ManoeuvreQueryResponse> basicList = manoeuvreMapper.selectBasicByParams(request);

        if (basicList.isEmpty()) {
            return basicList;
        }

        // 获取演习ID列表
        List<Long> manoeuvreIds = basicList.stream()
                .map(ManoeuvreQueryResponse::getId)
                .collect(Collectors.toList());

        // 查询检修员信息
        List<ManoeuvreRepairmanInfo> repairmanInfos = manoeuvreMapper.selectRepairmansByManoeuvreIds(manoeuvreIds);

        // 按演习ID分组检修员信息
        Map<Long, List<HashMap<String, Object>>> repairmanMap = repairmanInfos.stream()
                .collect(Collectors.groupingBy(
                        ManoeuvreRepairmanInfo::getManoeuvreId,
                        Collectors.mapping(info -> {
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("repairman_id", info.getRepairmanId());
                            map.put("repairman_name", info.getRepairmanName());
                            return map;
                        }, Collectors.toList())));

        // 设置检修员信息
        basicList.forEach(manoeuvre -> {
            List<HashMap<String, Object>> repairmans = repairmanMap.get(manoeuvre.getId());
            manoeuvre.setRepairmans(repairmans != null ? repairmans : new ArrayList<>());
        });

        log.info("查询到演习信息 {} 条", basicList.size());
        return basicList;
    }

    @Override
    public List<ManoeuvreQueryResponse> findManoeuvreById(Long id) {
        log.info("根据ID查询演习信息，演习ID：{}", id);

        // 查询基础演习信息
        ManoeuvreQueryResponse basicInfo = manoeuvreMapper.selectBasicById(id);

        if (basicInfo == null) {
            log.info("未找到ID为 {} 的演习信息", id);
            return new ArrayList<>();
        }

        // 查询检修员信息
        List<ManoeuvreRepairmanInfo> repairmanInfos = manoeuvreMapper.selectRepairmansByManoeuvreIds(Arrays.asList(id));

        // 设置检修员信息
        List<HashMap<String, Object>> repairmans = repairmanInfos.stream()
                .map(info -> {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("repairman_id", info.getRepairmanId());
                    map.put("repairman_name", info.getRepairmanName());
                    return map;
                })
                .collect(Collectors.toList());

        basicInfo.setRepairmans(repairmans);

        log.info("查询到演习信息：{}", basicInfo);
        return Arrays.asList(basicInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addManoeuvre(ManoeuvreAddRequest request) {
        log.info("添加演习信息，请求参数：{}", request);

        try {
            // 1. 插入演习基础信息
            manoeuvreMapper.insertManoeuvre(request);
            // 获取生成的主键ID
            Long manoeuvreId = request.getGeneratedId();
            log.info("插入演习基础信息成功，演习ID：{}", manoeuvreId);

            // 2. 插入演习与检修员的关联关系
            if (request.getRepairmans() != null && request.getRepairmans().length > 0) {
                manoeuvreMapper.insertManoeuvreRepairmanRelations(manoeuvreId, request.getRepairmans());
                log.info("插入演习检修员关联关系成功，演习ID：{}，检修员数量：{}", manoeuvreId, request.getRepairmans().length);
            }

            log.info("添加演习信息完成，演习ID：{}", manoeuvreId);
        } catch (Exception e) {
            log.error("添加演习信息失败：{}", e.getMessage(), e);
            throw new RuntimeException("添加演习信息失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateManoeuvre(ManoeuvreUpdateRequest request) {
        log.info("修改演习信息，请求参数：{}", request);

        try {
            // 1. 更新演习基础信息
            int updateCount = manoeuvreMapper.updateManoeuvre(request);
            if (updateCount == 0) {
                throw new RuntimeException("演习信息不存在或更新失败");
            }
            log.info("更新演习基础信息成功，演习ID：{}", request.getId());

            // 2. 更新演习与检修员的关联关系
            if (request.getRepairmans() != null) {
                // 先删除原有关联关系
                manoeuvreMapper.deleteManoeuvreRepairmanRelations(request.getId());
                log.info("删除原有演习检修员关联关系成功，演习ID：{}", request.getId());

                // 再插入新的关联关系
                if (request.getRepairmans().length > 0) {
                    manoeuvreMapper.insertManoeuvreRepairmanRelations(request.getId(), request.getRepairmans());
                    log.info("插入新的演习检修员关联关系成功，演习ID：{}，检修员数量：{}", request.getId(), request.getRepairmans().length);
                }
            }

            log.info("修改演习信息完成，演习ID：{}", request.getId());
        } catch (Exception e) {
            log.error("修改演习信息失败：{}", e.getMessage(), e);
            throw new RuntimeException("修改演习信息失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeManoeuvre(Long id) {
        log.info("删除演习信息，演习ID：{}", id);

        try {
            // 1. 先检查演习是否存在
            ManoeuvreQueryResponse existingManoeuvre = manoeuvreMapper.selectBasicById(id);
            if (existingManoeuvre == null) {
                throw new RuntimeException("演习信息不存在，无法删除");
            }

            // 2. 删除演习与检修员的关联关系
            manoeuvreMapper.deleteManoeuvreRepairmanRelations(id);
            log.info("删除演习检修员关联关系成功，演习ID：{}", id);

            // 3. 删除演习基础信息
            int deleteCount = manoeuvreMapper.deleteManoeuvre(id);
            if (deleteCount == 0) {
                throw new RuntimeException("演习信息删除失败");
            }
            log.info("删除演习基础信息成功，演习ID：{}", id);

            log.info("删除演习信息完成，演习ID：{}", id);
        } catch (Exception e) {
            log.error("删除演习信息失败：{}", e.getMessage(), e);
            throw new RuntimeException("删除演习信息失败：" + e.getMessage(), e);
        }
    }
}