package com.whut.friends.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author whut2024
 * @since 2024-09-02
 */
@Data
public class LoginRequest implements Serializable {


    private String userAccount;


    private String userPassword;


    private final static long serialVersionUID = -1L;

}
