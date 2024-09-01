package com.whut.friends.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建用户请求
 *
 */
@Data
public class UserAddRequest implements Serializable {


    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;


    private static final long serialVersionUID = 1L;
}