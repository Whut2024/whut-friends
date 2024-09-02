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
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionAddRequest;
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionQueryRequest;
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionRemoveRequest;
import com.whut.friends.model.entity.QuestionBankQuestion;
import com.whut.friends.model.entity.User;
import com.whut.friends.service.QuestionBankQuestionService;
import com.whut.friends.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库-题目接口
 */
@RestController
@RequestMapping("/questionBankQuestion")
@Slf4j
@AllArgsConstructor
public class QuestionBankQuestionController {


    private final QuestionBankQuestionService questionBankQuestionService;


    // region 增删改查

    /**
     * 创建题库-题目
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestionBankQuestion(@RequestBody QuestionBankQuestionAddRequest questionBankQuestionAddRequest) {
        ThrowUtils.throwIf(questionBankQuestionAddRequest == null, ErrorCode.PARAMS_ERROR);

        QuestionBankQuestion questionBankQuestion = BeanUtil.copyProperties(questionBankQuestionAddRequest,
                QuestionBankQuestion.class);

        // 数据校验
        questionBankQuestionService.validQuestionBankQuestion(questionBankQuestion, true);

        // 获取操作用户
        final User loginUser = UserHolder.get();
        questionBankQuestion.setUserId(loginUser.getId());

        // 写入数据库
        final boolean result = questionBankQuestionService.save(questionBankQuestion);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        final long newQuestionBankQuestionId = questionBankQuestion.getId();
        return ResultUtils.success(newQuestionBankQuestionId);
    }


    @PostMapping("/remove")
    public BaseResponse<Boolean> removeQuestionBankQuestion(@RequestBody QuestionBankQuestionRemoveRequest questionBankQuestionRemoveRequest) {
        ThrowUtils.throwIf(questionBankQuestionRemoveRequest == null, ErrorCode.PARAMS_ERROR);

        QuestionBankQuestion questionBankQuestion = BeanUtil.copyProperties(questionBankQuestionRemoveRequest,
                QuestionBankQuestion.class);

        // 数据校验
        questionBankQuestionService.validQuestionBankQuestion(questionBankQuestion, true);

        // 写入数据库
        LambdaQueryWrapper<QuestionBankQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionBankQuestion::getQuestionBankId, questionBankQuestion.getQuestionBankId());
        wrapper.eq(QuestionBankQuestion::getQuestionId, questionBankQuestion.getQuestionId());

        final boolean removed = questionBankQuestionService.remove(wrapper);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }


    // endregion
}
