package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 系统用户Mapper接口
 * 
 * @author parking-system
 * @since 2024-12-06
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    SysUser findByUsername(@Param("username") String username);


    /**
     * 更新最后登录信息
     * 
     * @param userId 用户ID
     * @param loginIp 登录IP
     */
    @Update("UPDATE sys_user SET last_login_time = NOW(), last_login_ip = #{loginIp}, " +
            "login_count = login_count + 1, update_time = NOW() WHERE id = #{userId}")
    void updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp);

    /**
     * 获取用户列表（支持关键字搜索）
     * 
     * @param keyword 搜索关键字
     * @return 用户列表
     */
    @Select("<script>" +
            "SELECT * FROM sys_user WHERE is_deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (username LIKE CONCAT('%', #{keyword}, '%') OR remark LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    java.util.List<SysUser> getUserList(@Param("keyword") String keyword);
}
