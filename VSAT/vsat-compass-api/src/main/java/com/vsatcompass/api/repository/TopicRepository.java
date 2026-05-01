package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findBySubjectIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(Long subjectId);

    boolean existsBySubjectId(Long subjectId);
}
