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
import com.whut.friends.model.dto.question.QuestionAddRequest;
import com.whut.friends.model.dto.question.QuestionEditRequest;
import com.whut.friends.model.dto.question.QuestionQueryRequest;
import com.whut.friends.model.dto.question.QuestionUpdateRequest;
import com.whut.friends.model.entity.Question;
import com.whut.friends.model.entity.User;
import com.whut.friends.model.enums.UserRoleEnum;
import com.whut.friends.model.vo.QuestionVO;
import com.whut.friends.service.QuestionService;
import com.whut.friends.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 题目接口
 *
 */
@RestController
@RequestMapping("/question")
@Slf4j
@AllArgsConstructor
public class QuestionController {
    
    
    private final QuestionService questionService;


    // region 增删改查

    /**
     * 创建题目
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);

        final Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        // 数据校验
        questionService.validQuestion(question, true);

        final User loginUser = UserHolder.get();
        question.setUserId(loginUser.getId());

        // 写入数据库
        final boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回新写入的数据 id
        final long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        final Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        // 获取当前用户
        final User user = UserHolder.get();

        // 判断是否存在
        final Question oldQuestion = questionService.getUserIdById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !UserRoleEnum.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 操作数据库
        final boolean result = questionService.removeQuestion(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用)
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        ThrowUtils.throwIf(questionUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        final Long id = questionUpdateRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        final Question question = BeanUtil.copyProperties(questionUpdateRequest, Question.class);

        // 数据校验
        questionService.validQuestion(question, false);

        // 判断是否存在
        final LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getId, id);
        final boolean existed = questionService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        final boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        final Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(QuestionVO.objToVo(question));
    }

    /**
     * 分页获取题目列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        // 查询数据库
        final Page<Question> questionPage = questionService.pageMayContainsBankId(questionQueryRequest);
        final Page<QuestionVO> questionVOPage = new Page<>();
        BeanUtil.copyProperties(questionPage, questionVOPage);
        return ResultUtils.success(questionVOPage);
    }

    /**
     * 编辑题目
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest) {
        ThrowUtils.throwIf(questionEditRequest == null, ErrorCode.PARAMS_ERROR);
        final Long id = questionEditRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        final Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);

        // 数据校验
        questionService.validQuestion(question, false);

        // 判断是否存在
        final LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getId, id);
        final boolean existed = questionService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        final boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
