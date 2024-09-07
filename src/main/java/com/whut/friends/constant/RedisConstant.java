package com.whut.friends.constant;

/**
 * Redis Key 相关常量
 * @author whut2024
 * @since 2024-09-07
 */
public interface RedisConstant {

    String SIGN_PREFIX = "user:sign:";


    static String getSignKey(String year, Long userId) {
        return String.format("%s:%s:%s", SIGN_PREFIX, year, userId);
    }

}
