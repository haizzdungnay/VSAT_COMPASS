package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionIdOrderByDisplayOrderAscIdAsc(Long questionId);

    void deleteByQuestionId(Long questionId);
}
