package com.whut.friends.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whut.friends.common.BaseResponse;
import com.whut.friends.common.DeleteRequest;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.common.ResultUtils;
import com.whut.friends.exception.BusinessException;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.model.dto.question.QuestionQueryRequest;
import com.whut.friends.model.dto.quetionbank.*;
import com.whut.friends.model.entity.Question;
import com.whut.friends.model.entity.QuestionBank;
import com.whut.friends.model.entity.User;
import com.whut.friends.model.enums.UserRoleEnum;
import com.whut.friends.model.vo.QuestionBankVO;
import com.whut.friends.model.vo.UserVO;
import com.whut.friends.service.QuestionBankService;
import com.whut.friends.service.QuestionService;
import com.whut.friends.service.UserService;
import com.whut.friends.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 题库接口
 */
@RestController
@RequestMapping("/questionBank")
@Slf4j
@AllArgsConstructor
public class QuestionBankController {


    private final QuestionBankService questionBankService;


    private final UserService userService;


    private final QuestionService questionService;


    // region 增删改查

    /**
     * 创建题库
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestionBank(@RequestBody QuestionBankAddRequest questionBankAddRequest) {
        ThrowUtils.throwIf(questionBankAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        final QuestionBank questionBank = BeanUtil.copyProperties(questionBankAddRequest, QuestionBank.class);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, true);
        // todo 填充默认值
        User loginUser = UserHolder.get();
        questionBank.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBankService.save(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBankId = questionBank.getId();
        return ResultUtils.success(newQuestionBankId);
    }

    /**
     * 删除题库
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestionBank(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        final Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        // 获取当前用户
        final User user = UserHolder.get();

        // 判断是否存在
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionBank::getId, id);
        final boolean existed = questionBankService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        // 仅管理员可删除
        if (!UserRoleEnum.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 操作数据库
        final boolean result = questionBankService.removeQuestionBank(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题库（仅管理员可用）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateQuestionBank(@RequestBody QuestionBankUpdateRequest questionBankUpdateRequest) {
        ThrowUtils.throwIf(questionBankUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        final Long id = questionBankUpdateRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        final QuestionBank questionBank = BeanUtil.copyProperties(questionBankUpdateRequest, QuestionBank.class);

        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);

        // 判断是否存在
        final LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionBank::getId, id);
        final boolean existed = questionBankService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        final boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题库（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(@ModelAttribute QuestionBankSelectQuestionRequest selectRequest) {
        // 校验
        ThrowUtils.throwIf(selectRequest == null, ErrorCode.PARAMS_ERROR);

        final Long id = selectRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        QuestionBank questionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取封装类
        final QuestionBankVO questionBankVO = QuestionBankVO.objToVo(questionBank);

        // 查询题目
        final Boolean whetherSelectQuestion = selectRequest.getNeedQueryQuestionList();
        if (whetherSelectQuestion != null && whetherSelectQuestion) {
            final QuestionQueryRequest questionQueryRequest = new QuestionQueryRequest();
            questionQueryRequest.setQuestionBankId(id);
            final Page<Question> questionPage = questionService.pageMayContainsBankId(questionQueryRequest);
            questionBankVO.setQuestionPage(questionPage);
        }

        // 查询创建人
        final Long userId = questionBankVO.getUserId();
        if (userId != null) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(User::getUserName).eq(User::getId, userId);
            final User user = userService.getOne(wrapper);
            questionBankVO.setUser(UserVO.objToVo(user));
        }

        return ResultUtils.success(questionBankVO);
    }


    /**
     * 分页获取题库列表（封装类）
     */
    @PostMapping(value = {"/list/page/vo", "/list/page"})
    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest) {
        final long current = questionBankQueryRequest.getCurrent();
        final long size = questionBankQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 1000, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        final Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size), questionBankService.getQueryWrapper(questionBankQueryRequest));

        final Page<QuestionBankVO> questionBankVOPage = new Page<>();
        BeanUtil.copyProperties(questionBankPage, questionBankVOPage);

        // 获取封装类
        return ResultUtils.success(questionBankVOPage);
    }


    /**
     * 编辑题库
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestionBank(@RequestBody QuestionBankEditRequest questionBankEditRequest) {
        ThrowUtils.throwIf(questionBankEditRequest == null, ErrorCode.PARAMS_ERROR);
        final Long id = questionBankEditRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        final QuestionBank questionBank = BeanUtil.copyProperties(questionBankEditRequest, QuestionBank.class);

        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);

        // 判断是否存在
        final LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionBank::getId, id);
        final boolean existed = questionBankService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        final boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
