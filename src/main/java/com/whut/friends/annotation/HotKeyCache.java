package com.whut.friends.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link com.whut.friends.aop.HotKeyInterceptor} <br>
 * {@link com.whut.friends.config.HotKeyConfig} <br>
 * 被注解的方法参数如果被判断为热点，则会直接使用本地缓存作返回值
 * @author whut2024
 * @since 2024-09-20
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HotKeyCache {

    String prefix() default "whut-friends";
}
