package com.whut.friends.mapper;

import com.whut.friends.model.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author laowang
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2024-09-01 09:11:17
* @Entity com.whut.friends.model.entity.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {


    /**
     * 根据用户ID获取问题信息
     *
     * @param id 用户ID
     * @return 返回一个包含用户ID对应问题信息的Question对象，如果未找到则返回null
     */
    Question getUserIdById(Long id);
}




