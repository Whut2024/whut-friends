package com.whut.friends.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whut.friends.common.BaseResponse;
import com.whut.friends.common.DeleteRequest;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.common.ResultUtils;
import com.whut.friends.esdto.QuestionEsDto;
import com.whut.friends.exception.BusinessException;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.model.dto.question.*;
import com.whut.friends.model.entity.Question;
import com.whut.friends.model.entity.User;
import com.whut.friends.model.enums.UserRoleEnum;
import com.whut.friends.model.vo.QuestionVO;
import com.whut.friends.service.QuestionService;
import com.whut.friends.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {


    private final QuestionService questionService;


    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }


    @Autowired(required = false)
    private ElasticsearchRestTemplate elasticsearchRestTemplate;


    // region 增删改查

    /**
     * 创建题目
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);

        final Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        // 数据校验
        questionService.validQuestion(question, true);

        final User loginUser = UserHolder.get();
        question.setUserId(loginUser.getId());

        // 写入数据库
        final boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回新写入的数据 id
        final long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        final Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        // 获取当前用户
        final User user = UserHolder.get();

        // 判断是否存在
        final Question oldQuestion = questionService.getUserIdById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !UserRoleEnum.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 操作数据库
        final boolean result = questionService.removeQuestion(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用)
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        ThrowUtils.throwIf(questionUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        final Long id = questionUpdateRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        final Question question = BeanUtil.copyProperties(questionUpdateRequest, Question.class);
        question.setTags(JSONUtil.toJsonStr(questionUpdateRequest.getTags()));

        // 数据校验
        questionService.validQuestion(question, false);

        // 判断是否存在
        final LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getId, id);
        final boolean existed = questionService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        final boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        final Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(QuestionVO.objToVo(question));
    }

    /**
     * 分页获取题目列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVoByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        // 查询数据库
        final Page<Question> questionPage = questionService.pageMayContainsBankId(questionQueryRequest);
        final Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());

        final List<QuestionVO> questionVOList = new ArrayList<>();
        final List<Question> questionList = questionPage.getRecords();
        for (Question question : questionList) {
            questionVOList.add(QuestionVO.objToVo(question));
        }
        questionVOPage.setRecords(questionVOList);

        return ResultUtils.success(questionVOPage);
    }


    /**
     * 分页获取题目列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        // 查询数据库
        final Page<Question> questionPage = questionService.pageMayContainsBankId(questionQueryRequest);

        return ResultUtils.success(questionPage);
    }


    /**
     * 编辑题目
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest) {
        ThrowUtils.throwIf(questionEditRequest == null, ErrorCode.PARAMS_ERROR);
        final Long id = questionEditRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        final Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);

        // 数据校验
        questionService.validQuestion(question, false);

        // 判断是否存在
        final LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getId, id);
        final boolean existed = questionService.getBaseMapper().exists(wrapper);
        ThrowUtils.throwIf(!existed, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        final boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion


    @PostMapping("/search/page/vo")
    public BaseResponse<Page<Question>> searchPageVo(@RequestBody QuestionQueryRequest questionQueryRequest) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);

        final int pageSize = questionQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 30, ErrorCode.PARAMS_ERROR);

        Page<Question> questionPage = null;

        if (elasticsearchRestTemplate != null) {
            FutureTask<Page<Question>> esTask = new FutureTask<>(() -> questionService.searchFromEs(questionQueryRequest));
            try {
                esTask.run();
                questionPage = esTask.get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn(e.toString());
            }
        }

        // 容错兜底
        if (questionPage == null)
            questionPage = questionService.pageMayContainsBankId(questionQueryRequest);

        return ResultUtils.success(questionPage);
    }


    @PostMapping("/delete/batch")
    public BaseResponse<Boolean> deleteBatch(@RequestBody QuestionMultiRequest removeRequest) {
        ThrowUtils.throwIf(removeRequest == null, ErrorCode.PARAMS_ERROR);
        final List<Long> questionIdList = removeRequest.getQuestionIdList();
        ThrowUtils.throwIf(questionIdList == null || CollectionUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "问题参数为NULL");

        questionService.deleteBatch(questionIdList);

        return ResultUtils.success(Boolean.TRUE);
    }
}
