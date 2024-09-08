package com.whut.friends.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whut.friends.annotation.DistributedLock;
import com.whut.friends.common.BaseResponse;
import com.whut.friends.common.DeleteRequest;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.common.ResultUtils;
import com.whut.friends.constant.RedisConstant;
import com.whut.friends.constant.UserConstant;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.model.dto.user.*;
import com.whut.friends.model.entity.User;
import com.whut.friends.model.enums.UserRoleEnum;
import com.whut.friends.model.vo.LoginUserVO;
import com.whut.friends.model.vo.UserVO;
import com.whut.friends.service.UserService;
import com.whut.friends.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
@AllArgsConstructor
public class UserController {


    private final UserService userService;


    private final StringRedisTemplate redisTemplate;


    private final RedissonClient redissonClient;

    // region 增删改查


    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody LoginRequest loginRequest) {
        // 校验
        ThrowUtils.throwIf(loginRequest == null, ErrorCode.PARAMS_ERROR);

        final String userAccount = loginRequest.getUserAccount();
        final String userPassword = loginRequest.getUserPassword();

        ThrowUtils.throwIf(StrUtil.isBlank(userAccount) || userAccount.length() > 16, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(userPassword) || userPassword.length() > 16, ErrorCode.PARAMS_ERROR);

        // 查库
        final LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserAccount, userAccount).eq(User::getUserPassword, userPassword);

        final User user = userService.getOne(wrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "账号或密码错误");

        // 存 Version
        final String cacheKey = UserConstant.USER_LOGIN_VERSION + user.getId();
        final String newVersion = String.valueOf(redisTemplate.opsForValue().increment(cacheKey));
        redisTemplate.expire(cacheKey, UserConstant.USER_LOGIN_VERSION_TTL, TimeUnit.MINUTES);

        // 生成 JWT
        final Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put(UserConstant.VERSION_KEY, newVersion);
        payloadMap.put(UserConstant.TTL, UserConstant.USER_LOGIN_TTL + System.currentTimeMillis());
        payloadMap.put(UserConstant.USER_KEY, user);
        final String token = JWTUtil.createToken(payloadMap, UserConstant.KEY_BYTES);

        final LoginUserVO loginUserVO = BeanUtil.copyProperties(user, LoginUserVO.class);
        loginUserVO.setToken(token);
        return ResultUtils.success(loginUserVO);
    }


    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser() {
        User user = UserHolder.get();
        if (user == null)
            return ResultUtils.success(new UserVO());


        return ResultUtils.success(BeanUtil.copyProperties(user, UserVO.class));
    }


    @PostMapping("/logout")
    public BaseResponse<Boolean> loginOut() {
        final User user = UserHolder.get();
        ThrowUtils.throwIf(user == null, ErrorCode.OPERATION_ERROR);

        final String cacheKey = UserConstant.USER_LOGIN_VERSION + user.getId();
        redisTemplate.opsForValue().increment(cacheKey);

        return ResultUtils.success(true);
    }


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
        final User loginUser = UserHolder.get();
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


    /**
     * 签到接口
     *
     * @return 签到结果，使用BaseResponse<Boolean>封装，其中Boolean表示签到是否成功
     */
    @PostMapping("/add/sign_in")
    public BaseResponse<Boolean> signIn() {
        // 获取当前用户
        final User user = UserHolder.get();

        // 获取分布式 bitset
        final int year = LocalDateTime.now().getYear();
        final String cacheKey = RedisConstant.getSignKey(String.valueOf(year), user.getId());
        final RBitSet bitSet = redissonClient.getBitSet(cacheKey);

        //校验当天登录情况
        final LocalDateTime localDateTime = LocalDateTime.now();
        final int dayOfYear = localDateTime.getDayOfYear();

        boolean hadSigned = bitSet.get(dayOfYear);
        if (!hadSigned)
            bitSet.set(dayOfYear);

        // 返回前端
        return ResultUtils.success(Boolean.TRUE);
    }


    /**
     * 获取年度签到记录
     *
     * @return 包含签到年份列表的BaseResponse对象
     */
    @GetMapping("/get/sign_in")
    public BaseResponse<List<Integer>> getSignIn(@RequestParam Integer year) {
        // 获取当前登录用户
        final User user = UserHolder.get();

        // 获取分布式bitset
        final String cacheKey = RedisConstant.getSignKey(String.valueOf(year), user.getId());
        final RBitSet bitSet = redissonClient.getBitSet(cacheKey);

        // 本地化bitset
        final BitSet localBitSet = bitSet.asBitSet();

        // 位运算计算签到日期
        int dayOfYear = 0;
        final List<Integer> dateList = new ArrayList<>();

        while ((dayOfYear = localBitSet.nextSetBit(dayOfYear + 1)) != -1)
            dateList.add(dayOfYear);

        // 返回前端
        return ResultUtils.success(dateList);
    }

    // endregion
}
