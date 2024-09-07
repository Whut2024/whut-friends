package com.whut.friends.esdto;

import com.whut.friends.model.entity.Question;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author whut2024
 * @since 2024-09-07
 */
public interface QuestionEsDto extends ElasticsearchRepository<Question, Long> {
}
