package com.whut.friends.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.friends.model.dto.user.UserQueryRequest;
import com.whut.friends.model.entity.User;
import com.whut.friends.model.vo.UserVO;

/**
 * 用户服务
 *
 */
public interface UserService extends IService<User> {

    /**
     * 校验数据
     * @param add 对创建的数据进行校验
     */
    void validUser(User user, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);


    /**
     * 分页获取用户封装
     */
    Page<UserVO> getUserVOPage(Page<User> userPage);
}
