package com.gxa.pipe.service.dataManagement;

import com.gxa.pipe.pojo.dto.dataManagement.repairman.*;
import com.gxa.pipe.pojo.vo.dataManagement.repairman.*;
import com.gxa.pipe.pojo.entity.Repairman;
import com.gxa.pipe.utils.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检修员服务接口
 */
public interface RepairmanService {

    /**
     * 分页查询检修员
     * 
     * @param request 查询请求
     * @return 查询响应
     */
    RepairmanPageQueryResponse queryRepairmen(RepairmanQueryRequest request);

    /**
     * 根据ID查询检修员
     * 
     * @param id 检修员ID
     * @return 检修员信息
     */
    Repairman getById(Long id);

    /**
     * 新增检修员
     * 
     * @param request 新增请求
     * @return 是否成功
     */
    boolean addRepairman(RepairmanAddRequest request);

    /**
     * 修改检修员
     * 
     * @param request 修改请求
     * @return 是否成功
     */
    boolean updateRepairman(RepairmanUpdateRequest request);

    /**
     * 删除检修员
     * 
     * @param id 检修员ID
     * @return 是否成功
     */
    boolean deleteRepairman(Long id);

    /**
     * 批量删除检修员
     * 
     * @param ids 检修员ID列表
     * @return 是否成功
     */
    boolean batchDeleteRepairmen(List<Long> ids);

    /**
     * 查询检修员指标
     * 
     * @param request 指标查询请求
     * @return 指标响应
     */
    RepairmanIndicatorResponse getRepairmanIndicator(RepairmanIndicatorRequest request);

    // 保留原有的方法以保持兼容性
    /**
     * 分页查询检修员（兼容性方法）
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    @Deprecated
    PageResult<Repairman> getByPage(RepairmanQueryRequest request);

    /**
     * 删除检修员（兼容性方法）
     * 
     * @param id 检修员ID
     * @return 是否成功
     */
    @Deprecated
    boolean delete(Long id);

    /**
     * 统计检修员总数
     * 
     * @return 检修员总数
     */
    int countTotal();

    /**
     * 计算平均完成时间
     * 
     * @return 平均完成时间（小时）
     */
    BigDecimal calculateAverageCompletionTime();
}