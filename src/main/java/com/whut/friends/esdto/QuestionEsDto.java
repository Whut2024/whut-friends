package com.whut.friends.esdto;

import com.whut.friends.model.dto.question.QuestionEs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.support.SimpleElasticsearchRepository;

/**
 * @author whut2024
 * @since 2024-09-07
 */
@ConditionalOnBean(SimpleElasticsearchRepository.class)
public interface QuestionEsDto extends ElasticsearchRepository<QuestionEs, Long> {
}
