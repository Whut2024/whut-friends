package com.whut.friends.constant;

import java.nio.charset.StandardCharsets;

/**
 * @author whut2024
 * @since 2024-09-02
 */
public interface UserConstant {


    String USER_LOGIN_VERSION = "user:login:version:";


    final Long USER_LOGIN_VERSION_TTL = 30L;


    Long USER_LOGIN_TTL = 1000L * 60 * 30;


    String USER_KEY = "user";


    String VERSION_KEY = "version";


    String TTL = "expire_time";


    byte[] KEY_BYTES = "KEY_BYTES".getBytes(StandardCharsets.UTF_8);
}
