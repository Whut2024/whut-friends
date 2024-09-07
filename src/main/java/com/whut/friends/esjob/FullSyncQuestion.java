package com.whut.friends.esjob;

import cn.hutool.core.collection.CollectionUtil;
import com.whut.friends.esdto.QuestionEsDto;
import com.whut.friends.mapper.QuestionMapper;
import com.whut.friends.model.dto.question.QuestionEs;
import com.whut.friends.model.entity.Question;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询数据库全部数据到ES
 *
 * @author whut2024
 * @since 2024-09-07
 */
@Slf4j
//@Component
@AllArgsConstructor
public class FullSyncQuestion {


    private final QuestionEsDto questionEsDto;


    private final QuestionMapper questionMapper;


    @PostConstruct
    void init() {
        // 获取全部数据
        final List<Question> questionList = questionMapper.getAfterUpdateTimeQuestion(new Date(0));
        if (CollectionUtil.isEmpty(questionList)) {
            log.info("没有数据需要全量同步");
            return;
        }

        final List<QuestionEs> questionEsList = questionList.stream().map(QuestionEs::objToDto).collect(Collectors.toList());

        final int total = questionList.size();

        // 分批同步到ES
        final int pageSize = 500;

        int cursor = 0, end;

        while (true) {
            end = cursor + pageSize;
            if (end >= total) {
                log.info("同步 {} ---> {}", cursor, total);
                questionEsDto.saveAll(questionEsList.subList(cursor, total));
                log.info("MySQL ES 同步完成");
                return;
            }

            questionEsDto.saveAll(questionEsList.subList(cursor, end));
            log.info("同步 {} ---> {}", cursor, end);
            cursor += pageSize;
        }
    }
}
