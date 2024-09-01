package com.whut.friends.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.constant.CommonConstant;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.mapper.UserMapper;
import com.whut.friends.model.dto.user.UserQueryRequest;
import com.whut.friends.model.entity.User;
import com.whut.friends.model.vo.UserVO;
import com.whut.friends.service.UserService;
import com.whut.friends.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 校验数据
     * @param add  对创建的数据进行校验
     */
    @Override
    public void validUser(User user, boolean add) {
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR);

        final String userAccount = user.getUserAccount();
        final String userPassword = user.getUserPassword();
        final String userName = user.getUserName();
        final String userAvatar = user.getUserAvatar();
        final String userProfile = user.getUserProfile();


        ThrowUtils.throwIf(StrUtil.isNotBlank(userName) && userName.length() > 80, ErrorCode.PARAMS_ERROR, "参数长度错误");
        ThrowUtils.throwIf(StrUtil.isNotBlank(userAvatar) && userAvatar.length() > 128, ErrorCode.PARAMS_ERROR, "参数长度错误");
        ThrowUtils.throwIf(StrUtil.isNotBlank(userProfile) && userProfile.length() > 80, ErrorCode.PARAMS_ERROR, "参数长度错误");


        // 创建数据时，参数不能为空
        ThrowUtils.throwIf(add && !StrUtil.isAllNotBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR);
    }

    /**
     * 获取查询条件
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (userQueryRequest == null) {
            return wrapper;
        }

        Long id = userQueryRequest.getId();

        if (id != null) {
            wrapper.eq("id", id);
            return wrapper;
        }

        final String userAccount = userQueryRequest.getUserAccount();
        final String userName = userQueryRequest.getUserName();
        final String userProfile = userQueryRequest.getUserProfile();
        final String userRole = userQueryRequest.getUserRole();
        final String sortField = userQueryRequest.getSortField();
        final String sortOrder = userQueryRequest.getSortOrder();

        wrapper.eq(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        wrapper.eq(StrUtil.isNotBlank(userName), "userName", userName);
        wrapper.eq(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        wrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);


        // 排序规则
        wrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return wrapper;
    }



    @Override
    public Page<UserVO> getUserVOPage(Page<User> userPage) {
        return null;
    }

}
