package com.gxa.pipe.mapper.dataManagement;

import com.gxa.pipe.pojo.dto.dataManagement.repairman.RepairmanQueryRequest;
import com.gxa.pipe.pojo.entity.Repairman;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 检修员Mapper接口
 */
@Mapper
public interface RepairmanMapper {

    /**
     * 分页查询检修员
     * 
     * @param request 查询请求
     * @return 检修员列表
     */
    List<Repairman> selectByPage(RepairmanQueryRequest request);

    /**
     * 根据ID查询检修员
     * 
     * @param id 检修员ID
     * @return 检修员信息
     */
    Repairman selectById(@Param("id") Long id);

    /**
     * 插入检修员
     * 
     * @param repairman 检修员信息
     * @return 影响行数
     */
    int insert(Repairman repairman);

    /**
     * 更新检修员
     * 
     * @param repairman 检修员信息
     * @return 影响行数
     */
    int update(Repairman repairman);

    /**
     * 删除检修员
     * 
     * @param id 检修员ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量删除检修员
     * 
     * @param ids 检修员ID列表
     * @return 影响行数
     */
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 统计检修员总数
     * 
     * @return 检修员总数
     */
    int countTotal();

    /**
     * 根据性别统计检修员数量
     * 
     * @param sex 性别（0-男，1-女）
     * @return 检修员数量
     */
    int countBySex(@Param("sex") String sex);

    /**
     * 根据区域ID统计检修员数量
     * 
     * @param areaId 区域ID
     * @return 检修员数量
     */
    int countByAreaId(@Param("areaId") String areaId);

    /**
     * 计算检修员平均年龄
     * 
     * @return 平均年龄
     */
    Double calculateAverageAge();

    /**
     * 计算平均完成时间
     * 
     * @return 平均完成时间（小时）
     */
    Double calculateAverageCompletionTime();
}