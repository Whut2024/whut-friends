package com.whut.friends.model.dto.questionbankquestion;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量删除题库-题目请求
 *
 */
@Data
public class QuestionBankQuestionMultiRequest implements Serializable {


    /**
     * 题库 id
     */
    private Long questionBankId;

    /**
     * 题目 id
     */
    private List<Long> questionIdList;


    private static final long serialVersionUID = 1L;
}