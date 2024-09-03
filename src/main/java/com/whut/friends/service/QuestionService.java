package com.whut.friends.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.friends.model.dto.question.QuestionQueryRequest;
import com.whut.friends.model.entity.Question;
import com.whut.friends.model.vo.QuestionVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * 题目服务
 *
 */
public interface QuestionService extends IService<Question> {

    /**
     * 校验数据
     * @param add 对创建的数据进行校验
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);


    /**
     * 根据用户ID获取问题信息
     *
     * @param id 用户ID
     * @return 返回一个问题对象，包含用户ID对应的问题信息；若不存在对应问题，则返回null
     */
    Question getUserIdById(Long id);


    /**
     * 从数据库中删除指定id的问题并删除题库关系。
     *
     * @param id 问题的id。
     * @return 如果删除成功返回true，否则返回false。
     */
    @Transactional
    boolean removeQuestion(Long id);


    /**
     * 分页查询可能包含题库编号的问题
     *
     * @param questionQueryRequest 问题查询请求对象，包含查询条件等信息
     * @return 包含题库编号的问题分页对象
     */
    Page<Question> pageMayContainsBankId(QuestionQueryRequest questionQueryRequest);
}
