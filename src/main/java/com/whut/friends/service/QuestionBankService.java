package com.whut.friends.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.friends.model.dto.quetionbank.QuestionBankQueryRequest;
import com.whut.friends.model.entity.QuestionBank;
import com.whut.friends.model.vo.QuestionBankVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * 题库服务
 *
 */
public interface QuestionBankService extends IService<QuestionBank> {

    /**
     * 校验数据
     * @param add 对创建的数据进行校验
     */
    void validQuestionBank(QuestionBank questionBank, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper<QuestionBank> getQueryWrapper(QuestionBankQueryRequest questionBankQueryRequest);


    /**
     * 根据ID删除问题库
     *
     * @param id 问题库的ID
     * @return 如果删除成功则返回true，否则返回false
     */
    @Transactional
    boolean removeQuestionBank(Long id);
}
