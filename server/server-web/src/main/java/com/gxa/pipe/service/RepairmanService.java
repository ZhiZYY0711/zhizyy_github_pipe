package com.gxa.pipe.service;

import com.gxa.pipe.pojo.dto.request.dataManagement.repairman.RepairmanQueryRequest;
import com.gxa.pipe.pojo.entity.Repairman;
import com.gxa.pipe.utils.PageResult;

import java.math.BigDecimal;

/**
 * 维修工服务接口
 */
public interface RepairmanService {

    /**
     * 分页查询维修工
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<Repairman> getByPage(RepairmanQueryRequest request);

    /**
     * 根据ID查询维修工
     * 
     * @param id 维修工ID
     * @return 维修工信息
     */
    Repairman getById(Long id);

    /**
     * 创建维修工
     * 
     * @param repairman 维修工信息
     * @return 是否成功
     */
    boolean create(Repairman repairman);

    /**
     * 更新维修工
     * 
     * @param repairman 维修工信息
     * @return 是否成功
     */
    boolean update(Repairman repairman);

    /**
     * 删除维修工
     * 
     * @param id 维修工ID
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 统计维修工总数
     * 
     * @return 维修工总数
     */
    int countTotal();

    /**
     * 计算平均完成时间
     * 
     * @return 平均完成时间（小时）
     */
    BigDecimal calculateAverageCompletionTime();
}