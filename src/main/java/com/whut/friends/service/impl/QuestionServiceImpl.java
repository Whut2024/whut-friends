package com.whut.friends.service.impl;

import cn.hutool.core.collection.CollUtil;
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
import com.whut.friends.model.dto.question.QuestionEs;
import com.whut.friends.model.dto.question.QuestionQueryRequest;
import com.whut.friends.model.entity.Question;
import com.whut.friends.model.entity.QuestionBankQuestion;
import com.whut.friends.model.vo.QuestionVO;
import com.whut.friends.service.QuestionBankQuestionService;
import com.whut.friends.service.QuestionService;
import com.whut.friends.utils.SqlUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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


    private final ElasticsearchRestTemplate elasticsearchRestTemplate;


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
            if (CollectionUtil.isNotEmpty(questionBankQuestionList)) {
                wrapper.in("id",
                        questionBankQuestionList.stream()
                                .map(QuestionBankQuestion::getQuestionId)
                                .collect(Collectors.toSet()));
            }
        }


        final String title = questionQueryRequest.getTitle();
        final String content = questionQueryRequest.getContent();
        final List<String> tagStrList = questionQueryRequest.getTags();
        final String answer = questionQueryRequest.getAnswer();
        final Long userId = questionQueryRequest.getUserId();
        final String sortField = questionQueryRequest.getSortField();
        final String sortOrder = questionQueryRequest.getSortOrder();
        final String searchText = questionQueryRequest.getSearchText();

        if (StrUtil.isNotBlank(searchText))
            wrapper.like("title", searchText).or().like("content", searchText);


        wrapper.or().like(StrUtil.isNotBlank(title), "title", title);
        wrapper.or().like(StrUtil.isNotBlank(content), "content", content);

        if (CollectionUtil.isNotEmpty(tagStrList)) {
            for (String tag : tagStrList)
                wrapper.like("tags", tag);

        }

        wrapper.or().like(StrUtil.isNotBlank(answer), "answer", answer);
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

    /**
     * 从 ES 查询题目
     */
    @Override
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {
        // 获取参数
        final Long id = questionQueryRequest.getId();
        final Long notId = questionQueryRequest.getNotId();
        final String searchText = questionQueryRequest.getSearchText();
        final List<String> tagStrList = questionQueryRequest.getTags();
        final Long questionBankId = questionQueryRequest.getQuestionBankId();
        final Long userId = questionQueryRequest.getUserId();
        // 注意，ES 的起始页为 0
        final int current = questionQueryRequest.getCurrent() - 1;
        final int pageSize = questionQueryRequest.getPageSize();
        final String sortField = questionQueryRequest.getSortField();
        final String sortOrder = questionQueryRequest.getSortOrder();

        // 构造查询条件
        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 过滤
        boolQueryBuilder.filter(QueryBuilders.termQuery("isDelete", 0));
        if (id != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("id", id));
        }
        if (notId != null) {
            boolQueryBuilder.mustNot(QueryBuilders.termQuery("id", notId));
        }
        if (userId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("userId", userId));
        }
        if (questionBankId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("questionBankId", questionBankId));
        }
        // 必须包含所有标签
        if (CollUtil.isNotEmpty(tagStrList)) {
            for (String tag : tagStrList) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tags", tag));
            }
        }
        // 按关键词检索
        if (StrUtil.isNotBlank(searchText)) {
            // title = '' or content = '' or answer = ''
            boolQueryBuilder.should(QueryBuilders.matchQuery("title", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("content", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("answer", searchText));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        // 排序
        SortBuilder<?> sortBuilder = SortBuilders.scoreSort();
        if (StrUtil.isNotBlank(sortField)) {
            sortBuilder = SortBuilders.fieldSort(sortField);
            sortBuilder.order(CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC);
        }
        // 分页
        final PageRequest pageRequest = PageRequest.of(current, pageSize);
        // 构造查询
        final NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withSorts(sortBuilder)
                .build();
        final SearchHits<QuestionEs> searchHits = elasticsearchRestTemplate.search(searchQuery, QuestionEs.class);
        // 复用 MySQL / MyBatis Plus 的分页对象，封装返回结果
        final Page<Question> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        final List<Question> resourceList = new ArrayList<>();
        if (searchHits.hasSearchHits()) {
            List<SearchHit<QuestionEs>> searchHitList = searchHits.getSearchHits();
            for (SearchHit<QuestionEs> questionEsDTOSearchHit : searchHitList) {
                resourceList.add(QuestionEs.dtoToObj(questionEsDTOSearchHit.getContent()));
            }
        }
        page.setRecords(resourceList);
        return page;
    }


}
