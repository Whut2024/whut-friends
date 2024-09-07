package com.whut.friends.model.dto.question;

import com.whut.friends.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 查询题目请求
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;


    /**
     * 搜索内容
     */
    private String searchText;


    /**
     * 内容
     */
    private String content;


    /**
     * 推荐答案
     */
    private String answer;

    /**
     * 创建用户 id
     */
    private Long userId;


    /**
     * 题库Id
     */
    private Long questionBankId;


    /**
     * 标签列表（json 数组）
     */
    private List<String> tags;


    /**
     * 不查询ID
     */
    private Long notId;


    private static final long serialVersionUID = 1L;
}