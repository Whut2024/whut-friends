package com.whut.friends.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionMultiRequest;
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionQueryRequest;
import com.whut.friends.model.entity.QuestionBankQuestion;

/**
 * 题库-题目服务
 */
public interface QuestionBankQuestionService extends IService<QuestionBankQuestion> {

    /**
     * 校验数据
     * @param add 对创建的数据进行校验
     */
    void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest);


    /**
     * 批量添加问题到题库
     *
     * @param addRequest 批量添加问题请求对象，包含要添加的问题列表等信息
     */
    void batchAddQuestionToBank(QuestionBankQuestionMultiRequest addRequest);


    /**
     * 从题库中批量移除问题
     *
     * @param removeRequest 批量移除问题请求对象，包含要移除的问题ID列表等信息
     */
    void batchRemoveQuestionFromBank(QuestionBankQuestionMultiRequest removeRequest);
}
