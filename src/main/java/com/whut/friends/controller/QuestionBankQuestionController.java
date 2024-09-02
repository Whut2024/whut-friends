package com.whut.friends.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whut.friends.common.BaseResponse;
import com.whut.friends.common.DeleteRequest;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.common.ResultUtils;
import com.whut.friends.exception.BusinessException;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionAddRequest;
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionQueryRequest;
import com.whut.friends.model.entity.QuestionBankQuestion;
import com.whut.friends.model.entity.User;
import com.whut.friends.service.QuestionBankQuestionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库-题目接口
 *
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
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
        BeanUtils.copyProperties(questionBankQuestionAddRequest, questionBankQuestion);
        // 数据校验
        questionBankQuestionService.validQuestionBankQuestion(questionBankQuestion, true);
        // todo 填充默认值
        User loginUser = null;
        questionBankQuestion.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBankQuestionService.save(questionBankQuestion);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBankQuestionId = questionBankQuestion.getId();
        return ResultUtils.success(newQuestionBankQuestionId);
    }


    // endregion
}
