package com.smartcollege.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcollege.entity.SemesterMarksSession;

@Repository
public interface SemesterMarksSessionRepository extends JpaRepository<SemesterMarksSession, Long> {

    List<SemesterMarksSession> findByStatus(String status);

    Optional<SemesterMarksSession> findFirstByStatusOrderByOpenedAtDesc(String status);

    List<SemesterMarksSession> findAllByOrderByCreatedAtDesc();
}
