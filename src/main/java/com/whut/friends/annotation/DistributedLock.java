package com.whut.friends.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 方法上加上该注解可增强为执行前加分布式锁
 * @author whut2024
 * @since 2024-09-08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DistributedLock {


    long waitTime() default 0L;


    long leaseTime() default -1L;


    TimeUnit timeUnit() default TimeUnit.MICROSECONDS;


    String key() default "";


    boolean needId() default false;
}
