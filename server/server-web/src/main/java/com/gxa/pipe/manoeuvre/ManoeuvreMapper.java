package com.gxa.pipe.manoeuvre;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 演习Mapper接口
 */
@Mapper
public interface ManoeuvreMapper {

    /**
     * 根据条件查询演习基础信息
     * 
     * @param request 查询请求
     * @return 演习基础信息列表
     */
    List<ManoeuvreQueryResponse> selectBasicByParams(
            @Param("request") ManoeuvreQueryRequest request,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 根据演习ID列表查询检修员信息
     * 
     * @param manoeuvreIds 演习ID列表
     * @return 检修员信息列表
     */
    List<ManoeuvreRepairmanInfo> selectRepairmansByManoeuvreIds(@Param("manoeuvreIds") List<Long> manoeuvreIds);

    /**
     * 根据ID查询演习基础信息
     * 
     * @param id 演习ID
     * @return 演习基础信息
     */
    ManoeuvreQueryResponse selectBasicById(@Param("id") Long id);

    /**
     * 插入演习信息
     * 
     * @param request 演习添加请求
     * @return 插入的记录数
     */
    int insertManoeuvre(ManoeuvreAddRequest request);

    /**
     * 插入演习与检修员的关联关系
     * 
     * @param manoeuvreId 演习ID
     * @param repairmanIds 检修员ID数组
     */
    void insertManoeuvreRepairmanRelations(@Param("manoeuvreId") Long manoeuvreId, @Param("repairmanIds") long[] repairmanIds);

    /**
     * 更新演习信息
     * 
     * @param request 演习更新请求
     * @return 更新的记录数
     */
    int updateManoeuvre(ManoeuvreUpdateRequest request);

    /**
     * 删除演习与检修员的关联关系
     * 
     * @param manoeuvreId 演习ID
     */
    void deleteManoeuvreRepairmanRelations(@Param("manoeuvreId") Long manoeuvreId);

    /**
     * 删除演习信息
     * 
     * @param id 演习ID
     * @return 删除的记录数
     */
    int deleteManoeuvre(@Param("id") Long id);
}
