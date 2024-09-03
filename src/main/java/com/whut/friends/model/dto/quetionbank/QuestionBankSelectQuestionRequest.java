package com.whut.friends.model.dto.quetionbank;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询题库请求
 *
 */
@Data
public class QuestionBankSelectQuestionRequest implements Serializable {


    private final Long id;


    private final Boolean whetherSelectQuestion;


    private static final long serialVersionUID = 1L;
}