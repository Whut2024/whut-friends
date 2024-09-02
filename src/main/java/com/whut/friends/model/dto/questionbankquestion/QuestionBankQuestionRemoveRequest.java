package com.whut.friends.model.dto.questionbankquestion;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建题库-题目请求
 *
 */
@Data
public class QuestionBankQuestionRemoveRequest implements Serializable {


    /**
     * 题目 id
     */
    private Long questionBankId;

    /**
     * 题目 id
     */
    private Long questionId;


    private static final long serialVersionUID = 1L;
}