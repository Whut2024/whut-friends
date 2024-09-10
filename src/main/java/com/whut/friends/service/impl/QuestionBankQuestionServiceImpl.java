package com.whut.friends.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.mapper.QuestionBankQuestionMapper;
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionMultiRequest;
import com.whut.friends.model.dto.questionbankquestion.QuestionBankQuestionQueryRequest;
import com.whut.friends.model.entity.Question;
import com.whut.friends.model.entity.QuestionBank;
import com.whut.friends.model.entity.QuestionBankQuestion;
import com.whut.friends.service.QuestionBankQuestionService;
import com.whut.friends.service.QuestionBankService;
import com.whut.friends.service.QuestionService;
import com.whut.friends.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 题库-题目服务实现
 */
@Service
@Slf4j

public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {


    @Lazy
    @Autowired
    private QuestionService questionService;


    @Autowired
    @Lazy
    private QuestionBankService questionBankService;


    /**
     * 校验数据
     *
     * @param add 对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);

        final Long questionBankId = questionBankQuestion.getQuestionBankId();
        final Long questionId = questionBankQuestion.getQuestionId();

        ThrowUtils.throwIf(questionBankId == null || questionId == null, ErrorCode.PARAMS_ERROR);

        boolean questionExisted = questionService.getBaseMapper().exists(new QueryWrapper<Question>().eq("id", questionId));
        ThrowUtils.throwIf(!questionExisted, ErrorCode.PARAMS_ERROR, "题目不存在");

        boolean questionBankExisted = questionBankService.getBaseMapper().exists(new QueryWrapper<QuestionBank>().eq("id", questionBankId));
        ThrowUtils.throwIf(!questionBankExisted, ErrorCode.PARAMS_ERROR, "题库不存在");
    }

    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        return null;
    }


    @Override
    public void batchAddQuestionToBank(QuestionBankQuestionMultiRequest addRequest) {
        // 参数基础校验
        beforeBatchChecking(addRequest);
        final Long questionBankId = addRequest.getQuestionBankId();
        List<Long> questionIdList = addRequest.getQuestionIdList();


        // 获取当前用户
        final Long userId = UserHolder.get().getId();

        // 加工数据，过滤已经存在的 题目 题库关联，生成新题目 题库关联
        final  Function<List<Long>, List<QuestionBankQuestion>> getQualifiedQbq = questionIdPartList -> {
            final LambdaQueryWrapper<QuestionBankQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                    .in(QuestionBankQuestion::getQuestionId, questionIdPartList)
                    .select(QuestionBankQuestion::getQuestionId);
            final List<Long> existentQuestionIdList = this.listObjs(wrapper, id -> (Long) id);
            final HashSet<Long> existentQuestionIdSet = new HashSet<>(existentQuestionIdList);

            return questionIdPartList.stream()
                    .filter(id -> !existentQuestionIdSet.contains(id))
                    .map(questionId -> {
                        final QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                        questionBankQuestion.setUserId(userId);
                        questionBankQuestion.setQuestionId(questionId);
                        questionBankQuestion.setQuestionBankId(questionBankId);
                        return questionBankQuestion;
                    }).collect(Collectors.toList());
        };

        // 当前代理对象
        final QuestionBankQuestionServiceImpl qbqServiceImpl = (QuestionBankQuestionServiceImpl) AopContext.currentProxy();

        batchOperation(questionIdList, getQualifiedQbq, qbqServiceImpl::saveBatch);

    }

    @Override
    public void batchRemoveQuestionFromBank(QuestionBankQuestionMultiRequest removeRequest) {
        // 基本校验
        beforeBatchChecking(removeRequest);

        final Long questionBankId = removeRequest.getQuestionBankId();
        List<Long> questionIdList = removeRequest.getQuestionIdList();

        // 加工数据，过滤不存在的 题目 题库关联，生成存在的新题目 题库关联 ID
        final Function<List<Long>, List<QuestionBankQuestion>> getQualifiedQbq = questionIdPartList -> {
            final LambdaQueryWrapper<QuestionBankQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                    .in(QuestionBankQuestion::getQuestionId, questionIdPartList)
                    .select(QuestionBankQuestion::getId);

            return this.list(wrapper);
        };

        // 当前代理对象
        final QuestionBankQuestionServiceImpl qbqServiceImpl = (QuestionBankQuestionServiceImpl) AopContext.currentProxy();

        batchOperation(questionIdList, getQualifiedQbq, qbqServiceImpl::removeBatchByIds);
    }


    protected void beforeBatchChecking(QuestionBankQuestionMultiRequest variableRequest) {
        // 基本校验
        ThrowUtils.throwIf(variableRequest == null, ErrorCode.PARAMS_ERROR, "参数为NULL");
        final Long questionBankId = variableRequest.getQuestionBankId();
        List<Long> questionIdList = variableRequest.getQuestionIdList();

        ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR, "qb id 为NULL");
        ThrowUtils.throwIf(CollectionUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "question id 为NULL");

    }


    protected void batchOperation(List<Long> questionIdList,
                                  Function<List<Long>, List<QuestionBankQuestion>> getQualifiedQbq,
                                  Function<List<QuestionBankQuestion>, Object> operation) {

        // 自定义创建 IO型线程池
        final ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
                4,                         // 核心线程数
                10,                        // 最大线程数
                60L,                       // 线程空闲存活时间
                TimeUnit.SECONDS,           // 存活时间单位
                new LinkedBlockingQueue<>(1000),  // 阻塞队列容量
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程处理任务
        );

        // 分批数据
        final int pageSize = 100;
        final int size = questionIdList.size();
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(size / pageSize + 1);

        for (int i = 0; i < size; i += pageSize) {
            final List<Long> questionIdPartList = questionIdList.subList(i, Math.min(i + pageSize, size));
            final List<QuestionBankQuestion> qbqList = getQualifiedQbq.apply(questionIdPartList);
            futureList.add(CompletableFuture.runAsync(() -> operation.apply(qbqList), customExecutor));
        }

        // 并发执行
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        customExecutor.shutdown();
    }
}
