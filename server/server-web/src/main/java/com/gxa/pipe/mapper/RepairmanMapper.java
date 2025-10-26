package com.gxa.pipe.mapper;

import com.gxa.pipe.pojo.entity.Repairman;
import com.gxa.pipe.pojo.dto.request.RepairmanQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 维修工Mapper接口
 */
@Mapper
public interface RepairmanMapper {
    
    /**
     * 分页查询维修工
     * @param request 查询请求
     * @return 维修工列表
     */
    List<Repairman> selectByPage(RepairmanQueryRequest request);
    
    /**
     * 根据ID查询维修工
     * @param id 维修工ID
     * @return 维修工信息
     */
    Repairman selectById(@Param("id") Long id);
    
    /**
     * 插入维修工
     * @param repairman 维修工信息
     * @return 影响行数
     */
    int insert(Repairman repairman);
    
    /**
     * 更新维修工
     * @param repairman 维修工信息
     * @return 影响行数
     */
    int update(Repairman repairman);
    
    /**
     * 删除维修工
     * @param id 维修工ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 统计维修工总数
     * @return 维修工总数
     */
    int countTotal();
    
    /**
     * 计算平均完成时间
     * @return 平均完成时间（小时）
     */
    Double calculateAverageCompletionTime();
}