package com.whut.friends.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.constant.CommonConstant;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.mapper.QuestionMapper;
import com.whut.friends.model.dto.question.QuestionQueryRequest;
import com.whut.friends.model.entity.Question;
import com.whut.friends.model.entity.QuestionBankQuestion;
import com.whut.friends.model.vo.QuestionVO;
import com.whut.friends.service.QuestionBankQuestionService;
import com.whut.friends.service.QuestionService;
import com.whut.friends.utils.SqlUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 */
@Service
@Slf4j
@AllArgsConstructor
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {


    private final QuestionBankQuestionService questionBankQuestionService;


    /**
     * 校验数据
     *
     * @param add 对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);

        final Long id = question.getId();
        final String title = question.getTitle();
        final String content = question.getContent();
        final String tags = question.getTags();
        final String answer = question.getAnswer();
        final Long userId = question.getUserId();

        ThrowUtils.throwIf(!add && (id == null || id <= 0), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!add && userId != null && userId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isNotBlank(title) && title.length() > 80, ErrorCode.PARAMS_ERROR, "参数长度错误");
        ThrowUtils.throwIf(StrUtil.isNotBlank(content) && content.length() > 80, ErrorCode.PARAMS_ERROR, "参数长度错误");
        ThrowUtils.throwIf(StrUtil.isNotBlank(tags) && tags.length() > 80, ErrorCode.PARAMS_ERROR, "参数长度错误");
        ThrowUtils.throwIf(StrUtil.isNotBlank(answer) && answer.length() > 80, ErrorCode.PARAMS_ERROR, "参数长度错误");
    }

    /**
     * 获取查询条件
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return wrapper;
        }

        final Long id = questionQueryRequest.getId();
        final Long questionBankId = questionQueryRequest.getQuestionBankId();
        if (id != null) {
            wrapper.eq("id", id);
            return wrapper;
        }

        if (questionBankId != null) {
            LambdaQueryWrapper<QuestionBankQuestion> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(QuestionBankQuestion::getQuestionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);

            List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionService.list(queryWrapper);
            if (!CollectionUtil.isNotEmpty(questionBankQuestionList)) {
                wrapper.in("id",
                        questionBankQuestionList.stream()
                                .map(QuestionBankQuestion::getQuestionId)
                                .collect(Collectors.toSet()));
            }
        }


        final String title = questionQueryRequest.getTitle();
        final String content = questionQueryRequest.getContent();
        final String tags = questionQueryRequest.getTags();
        final String answer = questionQueryRequest.getAnswer();
        final Long userId = questionQueryRequest.getUserId();
        final String sortField = questionQueryRequest.getSortField();
        final String sortOrder = questionQueryRequest.getSortOrder();

        wrapper.eq(StrUtil.isNotBlank(title), "title", title);
        wrapper.eq(StrUtil.isNotBlank(content), "content", content);
        wrapper.eq(StrUtil.isNotBlank(tags), "tags", tags);
        wrapper.eq(StrUtil.isNotBlank(answer), "answer", answer);
        wrapper.eq(userId != null, "userId", userId);


        // 排序规则
        wrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return wrapper;
    }


    @Override
    public Question getUserIdById(Long id) {
        return this.baseMapper.getUserIdById(id);
    }


    @Override
    public boolean removeQuestion(Long id) {
        final boolean removedQuestion = this.removeById(id);
        ThrowUtils.throwIf(!removedQuestion, ErrorCode.OPERATION_ERROR);

        final LambdaQueryWrapper<QuestionBankQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionBankQuestion::getQuestionId, id);
        final boolean removedQBQ = questionBankQuestionService.remove(wrapper);
        ThrowUtils.throwIf(!removedQBQ, ErrorCode.OPERATION_ERROR);

        return true;
    }


    @Override
    public Page<Question> pageMayContainsBankId(QuestionQueryRequest questionQueryRequest) {
        final int pageSize = questionQueryRequest.getPageSize();
        final int current = questionQueryRequest.getCurrent();

        return this.page(new Page<>(current, pageSize),
                this.getQueryWrapper(questionQueryRequest));
    }


}
