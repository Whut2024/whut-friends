package com.whut.friends.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whut.friends.common.BaseResponse;
import com.whut.friends.common.DeleteRequest;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.common.ResultUtils;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.model.dto.user.UserAddRequest;
import com.whut.friends.model.dto.user.UserEditRequest;
import com.whut.friends.model.dto.user.UserQueryRequest;
import com.whut.friends.model.dto.user.UserUpdateRequest;
import com.whut.friends.model.entity.User;
import com.whut.friends.model.enums.UserRoleEnum;
import com.whut.friends.model.vo.UserVO;
import com.whut.friends.service.UserService;
import com.whut.friends.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
@AllArgsConstructor
public class UserController {


    private final UserService userService;

    // region 增删改查

    /**
     * 创建用户
     */
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        final User user = BeanUtil.copyProperties(userAddRequest, User.class);
        // 数据校验
        userService.validUser(user, true);
        // 写入数据库
        final boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        final long newUserId = user.getId();
        return ResultUtils.success(newUserId);
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);

        final Long id = deleteRequest.getId();

        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        // 判断是否存在
        final LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getId, id);
        final boolean existed = userService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        final User user = UserHolder.get();
        // 仅本人或管理员可删除
        ThrowUtils.throwIf(!(id.equals(user.getId()) || UserRoleEnum.isAdmin(user)), ErrorCode.NO_AUTH_ERROR);

        // 操作数据库
        boolean result = userService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户（仅管理员可用）
     *
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);

        final Long id = userUpdateRequest.getId();

        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        final User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        // 数据校验
        userService.validUser(user, false);

        // 判断是否存在
        final LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getId, id);
        final boolean existed = userService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        final boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        final User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取封装类
        return ResultUtils.success(UserVO.objToVo(user));
    }

    /**
     * 分页获取用户列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 查询数据库
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        // 获取封装类
        return ResultUtils.success(userService.getUserVOPage(userPage));
    }

    /**
     * 分页获取当前登录用户创建的用户列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<UserVO>> listMyUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = null;
        userQueryRequest.setId(loginUser.getId());
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        // 获取封装类
        return ResultUtils.success(userService.getUserVOPage(userPage));
    }

    /**
     * 编辑用户（给用户使用)
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUser(@RequestBody UserEditRequest userEditRequest) {
        ThrowUtils.throwIf(userEditRequest == null, ErrorCode.PARAMS_ERROR);

        final Long id = userEditRequest.getId();

        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        final User user = new User();
        BeanUtils.copyProperties(userEditRequest, user);
        // 数据校验
        userService.validUser(user, false);

        // 判断是否存在
        final LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getId, id);
        final boolean existed = userService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        final boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
