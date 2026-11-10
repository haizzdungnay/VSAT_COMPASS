package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.QuestionMatchingChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionMatchingChoiceRepository extends JpaRepository<QuestionMatchingChoice, Long> {

    List<QuestionMatchingChoice> findByQuestionIdOrderByDisplayOrderAsc(Long questionId);

    void deleteByQuestionId(Long questionId);
}
