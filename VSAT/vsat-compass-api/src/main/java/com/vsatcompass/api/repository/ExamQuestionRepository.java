package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    long countByExamId(Long examId);

    boolean existsByExamIdAndQuestionId(Long examId, Long questionId);

    List<ExamQuestion> findByExamIdOrderByQuestionOrderAscIdAsc(Long examId);

    @Query("select coalesce(max(eq.questionOrder), 0) from ExamQuestion eq where eq.examId = :examId")
    Integer findMaxQuestionOrderByExamId(@Param("examId") Long examId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ExamQuestion eq where eq.examId = :examId and eq.questionId = :questionId")
    int deleteByExamIdAndQuestionId(
            @Param("examId") Long examId,
            @Param("questionId") Long questionId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ExamQuestion eq
            set eq.questionOrder = -1 * (eq.questionOrder + 1)
            where eq.examId = :examId
            """)
    int moveQuestionOrdersToTemporaryNegativeRange(@Param("examId") Long examId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ExamQuestion eq
            set eq.questionOrder = :questionOrder
            where eq.examId = :examId and eq.questionId = :questionId
            """)
    int updateQuestionOrder(
            @Param("examId") Long examId,
            @Param("questionId") Long questionId,
            @Param("questionOrder") Integer questionOrder);
}
