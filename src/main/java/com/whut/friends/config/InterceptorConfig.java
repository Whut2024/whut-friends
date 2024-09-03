package com.whut.friends.config;

import com.whut.friends.interceptor.LogInterceptor;
import com.whut.friends.interceptor.LoginInterceptor;
import com.whut.friends.interceptor.RoleInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用于添 Spring MVC 拦截器
 *
 * @author whut2024
 * @since 2024-09-01
 */
@Configuration
@AllArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {


    private final StringRedisTemplate redisTemplate;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor()).order(0);

        registry.addInterceptor(new LoginInterceptor(redisTemplate)).order(1);

        // todo 添加需要身份校验的路径
        /*registry.addInterceptor(new RoleInterceptor()).order(2).addPathPatterns(

        );*/
    }
}
