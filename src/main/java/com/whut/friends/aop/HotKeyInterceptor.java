package com.whut.friends.aop;

import cn.hutool.crypto.SecureUtil;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.whut.friends.annotation.HotKeyCache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * {@link HotKeyCache}
 * <p> 拦截被 @HotKeyCache 注解的方法，以方法参数进行MD5摘要后取前10位配合上注解提过的Key前缀组成缓存的Key </p>
 * <p> Value 通过 Caffeine 缓存在本地 </p>
 * @author whut2024
 * @since 2024-09-20
 */
@Slf4j
@Aspect
@Component
public class HotKeyInterceptor {


    @Around("@annotation(hotKeyCache)")
    public Object cache(ProceedingJoinPoint point, HotKeyCache hotKeyCache) throws Throwable {
        final String prefix = hotKeyCache.prefix();
        final Object arg = point.getArgs()[0];

        final String cacheKey = prefix + SecureUtil.md5(arg.toString()).substring(0, 17);

        // 热点判断
        if (JdHotKeyStore.isHotKey(cacheKey)) {

            log.info("{} 现在为热点数据", arg);

            // 可能缓存获取
            final Object cacheObject = JdHotKeyStore.get(cacheKey);
            if (cacheObject != null)
                return cacheObject;
        }

        // 调用方法获取数据，即查询数据库
        log.info("{} 直接查询数据库", arg);
        final Object result = point.proceed();

        // 智能缓存
        JdHotKeyStore.smartSet(cacheKey, result);

        return result;

    }
}
