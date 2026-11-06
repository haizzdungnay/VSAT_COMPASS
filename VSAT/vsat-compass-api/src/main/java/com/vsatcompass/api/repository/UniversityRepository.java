package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {
    Optional<University> findByCode(String code);
    List<University> findAllByOrderByCodeAsc();
}
