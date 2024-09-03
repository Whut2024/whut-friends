package com.whut.friends.utils;

import com.whut.friends.common.ErrorCode;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.model.entity.User;

/**
 * @author whut2024
 * @since 2024-09-01
 */
public class UserHolder {


    private final static ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();


    public static void set(User user) {
        USER_THREAD_LOCAL.set(user);
    }


    public static User get() {
        return USER_THREAD_LOCAL.get();
    }


    public static void remove() {
        USER_THREAD_LOCAL.remove();
    }
}
