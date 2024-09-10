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
        registry.addInterceptor(new LogInterceptor());

        registry.addInterceptor(new LoginInterceptor(redisTemplate));

        registry.addInterceptor(new RoleInterceptor()).addPathPatterns(
                // user
                "/user/add",
                "/user/delete",
                "/user/list/page",
                "/user/list/page/vo",
                "/user/my/list/page/vo",

                // question
                "/question/add",
                "/question/delete",
                "/question/edit",
                "/question/update",

                // question bank
                "/questionBank/add",
                "/questionBank/delete",
                "/questionBank/edit",
                "/questionBank/update",

                // question bank question
                "/questionBankQuestion/add",
                "/questionBankQuestion/remove",
                "/questionBankQuestion/add/batch",
                "/questionBankQuestion/remove/batch"
        );
    }
}
