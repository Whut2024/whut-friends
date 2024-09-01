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
import com.whut.friends.model.dto.quetionbank.QuestionBankAddRequest;
import com.whut.friends.model.dto.quetionbank.QuestionBankEditRequest;
import com.whut.friends.model.dto.quetionbank.QuestionBankQueryRequest;
import com.whut.friends.model.dto.quetionbank.QuestionBankUpdateRequest;
import com.whut.friends.model.entity.QuestionBank;
import com.whut.friends.model.entity.User;
import com.whut.friends.model.enums.UserRoleEnum;
import com.whut.friends.model.vo.QuestionBankVO;
import com.whut.friends.service.QuestionBankService;
import com.whut.friends.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 题库接口
 */
@RestController
@RequestMapping("/questionBank")
@Slf4j
@AllArgsConstructor
public class QuestionBankController {


    private final QuestionBankService questionBankService;


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
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        QuestionBank questionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(QuestionBankVO.objToVo(questionBank));
    }

    /**
     * 分页获取题库列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<QuestionBank>> listQuestionBankByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest) {
        final long current = questionBankQueryRequest.getCurrent();
        final long size = questionBankQueryRequest.getPageSize();

        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        return ResultUtils.success(questionBankPage);
    }

    /**
     * 分页获取题库列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                       HttpServletRequest request) {
        final long current = questionBankQueryRequest.getCurrent();
        final long size = questionBankQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));

        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage));
    }

    /**
     * 分页获取当前登录用户创建的题库列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listMyQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest) {
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 补充查询条件，只查询当前登录用户的数据
        final User loginUser = UserHolder.get();
        questionBankQueryRequest.setUserId(loginUser.getId());
        final long current = questionBankQueryRequest.getCurrent();
        final long size = questionBankQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));

        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage));
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
