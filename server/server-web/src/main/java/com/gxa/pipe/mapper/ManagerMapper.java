package com.gxa.pipe.mapper;

import com.gxa.pipe.pojo.entity.Manager;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 管理员Mapper接口
 */
@Mapper
public interface ManagerMapper {
    
    /**
     * 根据用户名查询管理员
     * @param username 用户名
     * @return 管理员信息
     */
    Manager selectByUsername(@Param("username") String username);
    
    /**
     * 根据ID查询管理员
     * @param id 管理员ID
     * @return 管理员信息
     */
    Manager selectById(@Param("id") Long id);
    
    /**
     * 插入管理员
     * @param manager 管理员信息
     * @return 影响行数
     */
    int insert(Manager manager);
    
    /**
     * 更新管理员
     * @param manager 管理员信息
     * @return 影响行数
     */
    int update(Manager manager);
    
    /**
     * 删除管理员
     * @param id 管理员ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}