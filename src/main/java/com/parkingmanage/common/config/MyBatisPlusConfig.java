package com.parkingmanage.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MyBatisPlusConfig {

    /**
     * 分页插件--乐观锁
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        //乐观锁
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     *
     *
     *
     * 缓存自动填充创建人 创建时间  修改人 修改时间
     * @return
     */

    // @Bean
    // public MetaObjectHandler metaObjectHandler() {
    //     return new MetaObjectHandler() {
    //         @Override
    //         public void insertFill(MetaObject metaObject) {
    //             try {
    //                 HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    //                 User user = (User) request.getSession().getAttribute("user");
    //                 if (!Objects.isNull(user)) {
    //                     this.setFieldValByName("createUser", user.getLoginName(), metaObject);
    //                 } else {
    //                     this.setFieldValByName("createUser", "admin", metaObject);
    //                 }
    //                 Object createTime = this.getFieldValByName("createTime", metaObject);
    //                 if (Objects.isNull(createTime)) {
    //                     this.setFieldValByName("createTime", LocalDateTime.now(), metaObject);
    //                 }
    //             } catch (Exception e) {
    //                 log.error("自动填充-获取用户session失败", e);
    //             }
    //         }
    //
    //         @Override
    //         public void updateFill(MetaObject metaObject) {
    //             try {
    //                 HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    //                 User user = (User) request.getSession().getAttribute("user");
    //                 if (!Objects.isNull(user)) {
    //                     this.setFieldValByName("updateUser", user.getLoginName(), metaObject);
    //                 } else {
    //                     this.setFieldValByName("updateUser", "admin", metaObject);
    //                 }
    //                 this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    //             } catch (Exception e) {
    //                 log.error("自动填充-获取用户session失败", e);
    //             }
    //         }
    //     };
    // }
}