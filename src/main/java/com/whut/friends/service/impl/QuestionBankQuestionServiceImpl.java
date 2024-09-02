package com.whut.friends.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.mapper.QuestionBankQuestionMapper;
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionQueryRequest;
import com.whut.friends.model.entity.Question;
import com.whut.friends.model.entity.QuestionBank;
import com.whut.friends.model.entity.QuestionBankQuestion;
import com.whut.friends.service.QuestionBankQuestionService;
import com.whut.friends.service.QuestionBankService;
import com.whut.friends.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 题库-题目服务实现
 *
 */
@Service
@Slf4j

public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {


    @Lazy
    @Autowired
    private  QuestionService questionService;


    private final QuestionBankService questionBankService;

    public QuestionBankQuestionServiceImpl(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    /**
     * 校验数据
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);

        final Long questionBankId = questionBankQuestion.getQuestionBankId();
        final Long questionId = questionBankQuestion.getQuestionId();

        ThrowUtils.throwIf(questionBankId == null || questionId == null, ErrorCode.PARAMS_ERROR);

        boolean questionExisted = questionService.getBaseMapper().exists(new QueryWrapper<Question>().eq("id", questionId));
        ThrowUtils.throwIf(!questionExisted, ErrorCode.PARAMS_ERROR, "题目不存在");

        boolean questionBankExisted = questionBankService.getBaseMapper().exists(new QueryWrapper<QuestionBank>().eq("id", questionBankId));
        ThrowUtils.throwIf(!questionBankExisted, ErrorCode.PARAMS_ERROR, "题库不存在");
    }

    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        return null;
    }


}
