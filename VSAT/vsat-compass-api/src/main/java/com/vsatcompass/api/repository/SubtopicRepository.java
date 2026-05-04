package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.Subtopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubtopicRepository extends JpaRepository<Subtopic, Long> {

    Optional<Subtopic> findByCode(String code);

    List<Subtopic> findByTopicIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(Long topicId);
}
