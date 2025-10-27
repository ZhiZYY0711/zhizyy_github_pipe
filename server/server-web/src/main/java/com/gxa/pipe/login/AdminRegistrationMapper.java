package com.gxa.pipe.login;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.gxa.pipe.entity.AdminRegistration;

@Mapper
public interface AdminRegistrationMapper {

    /**
     * 根据用户名查询管理员信息
     * 
     * @param username 用户名
     * @return 管理员信息
     */
    @Select("SELECT * FROM admin_registration WHERE username = #{username}")
    AdminRegistration findByUsername(String username);

    /**
     * 更新管理员最后登录时间
     * 
     * @param id            管理员ID
     * @param lastLoginTime 最后登录时间
     */
    @Select("UPDATE admin_registration SET last_login_time = #{lastLoginTime} WHERE id = #{id}")
    void updateLastLoginTime(Long id, java.time.LocalDateTime lastLoginTime);
}
