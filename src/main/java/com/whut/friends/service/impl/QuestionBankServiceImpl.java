package com.whut.friends.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.friends.common.ErrorCode;
import com.whut.friends.constant.CommonConstant;
import com.whut.friends.exception.ThrowUtils;
import com.whut.friends.mapper.QuestionBankMapper;
import com.whut.friends.model.dto.quetionbank.QuestionBankQueryRequest;
import com.whut.friends.model.entity.QuestionBank;
import com.whut.friends.model.vo.QuestionBankVO;
import com.whut.friends.service.QuestionBankService;
import com.whut.friends.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 题库服务实现
 */
@Service
@Slf4j
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements QuestionBankService {


    /**
     * 校验数据
     * @param add          对创建的数据进行校验
     */
    @Override
    public void validQuestionBank(QuestionBank questionBank, boolean add) {
        ThrowUtils.throwIf(questionBank == null, ErrorCode.PARAMS_ERROR);

        final Long id = questionBank.getId();
        final String title = questionBank.getTitle();
        final String description = questionBank.getDescription();
        final String picture = questionBank.getPicture();
        final Long userId = questionBank.getUserId();


        ThrowUtils.throwIf(!add && (id == null || id <= 0), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!add && userId != null && userId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isNotBlank(title) && title.length() > 80, ErrorCode.PARAMS_ERROR, "参数长度错误");
        ThrowUtils.throwIf(StrUtil.isNotBlank(description) && description.length() > 80, ErrorCode.PARAMS_ERROR, "参数长度错误");
        ThrowUtils.throwIf(StrUtil.isNotBlank(picture) && picture.length() > 128, ErrorCode.PARAMS_ERROR, "参数长度错误");
    }

    /**
     * 获取查询条件
     */
    @Override
    public QueryWrapper<QuestionBank> getQueryWrapper(QuestionBankQueryRequest questionBankQueryRequest) {
        QueryWrapper<QuestionBank> wrapper = new QueryWrapper<>();
        if (questionBankQueryRequest == null) {
            return wrapper;
        }

        final Long id = questionBankQueryRequest.getId();
        if (id != null) {
            wrapper.eq("id", id);
            return wrapper;
        }

        final String title = questionBankQueryRequest.getTitle();
        final String description = questionBankQueryRequest.getDescription();
        final String picture = questionBankQueryRequest.getPicture();
        final Long userId = questionBankQueryRequest.getUserId();
        final String sortField = questionBankQueryRequest.getSortField();
        final String sortOrder = questionBankQueryRequest.getSortOrder();

        wrapper.eq(StrUtil.isNotBlank(title), "title", title);
        wrapper.eq(StrUtil.isNotBlank(description), "description", description);
        wrapper.eq(StrUtil.isNotBlank(picture), "picture", picture);
        wrapper.eq(userId != null, "userId", userId);

        // 排序规则
        wrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return wrapper;
    }



    @Override
    public Page<QuestionBankVO> getQuestionBankVOPage(Page<QuestionBank> questionBankPage) {
        return null;
    }

    // todo 联合删除 题库-题目 表
    @Override
    public boolean removeQuestionBank(Long id) {
        final boolean removedQuestionBank = this.removeById(id);
        ThrowUtils.throwIf(!removedQuestionBank, ErrorCode.OPERATION_ERROR);

        return removedQuestionBank;
    }


}
