package com.whut.friends.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量题目请求
 *
 */
@Data
public class QuestionMultiRequest implements Serializable {


    private List<Long> questionIdList;


    private static final long serialVersionUID = 1L;
}