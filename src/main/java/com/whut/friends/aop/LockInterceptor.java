package com.whut.friends.aop;

import com.whut.friends.annotation.DistributedLock;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.model.entity.User;
import com.whut.friends.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 增强需要整体加锁的方法
 *
 * @author whut2024
 * @since 2024-09-08
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class LockInterceptor {


    private final RedissonClient redissonClient;


    @Around("@annotation(lockArgs)")
    public Object lock(ProceedingJoinPoint point, DistributedLock lockArgs) {
        final long leaseTime = lockArgs.leaseTime();
        final long waitTime = lockArgs.waitTime();
        final TimeUnit timeUnit = lockArgs.timeUnit();
        String key = lockArgs.key();
        final boolean needId = lockArgs.needId();

        // 是否加上用户ID限制
        if (needId) {
            final User user = UserHolder.get();
            ThrowUtils.throwIf(user == null || user.getId() == null, ErrorCode.NOT_LOGIN_ERROR);

            key += user.getId();
        }

        try {
            // 加锁
            final RLock lock = redissonClient.getLock(key);

            final boolean locked = lock.tryLock(waitTime, leaseTime, timeUnit);

            // 输出日志
            log.info("加锁 {} : {}", locked, point.getSignature());

            if (!locked)
                return null;

            // 执行
            final Object result;

            try {
                result = point.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.warn("释放锁失败 {}", lock.getName());
                }
            }

            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


}
