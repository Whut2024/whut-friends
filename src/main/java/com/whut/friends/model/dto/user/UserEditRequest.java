package com.whut.friends.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑用户请求
 *
 */
@Data
public class UserEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;


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