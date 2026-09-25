package com.smartcollege.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcollege.entity.AttendanceSession;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findBySessionToken(String sessionToken);

    List<AttendanceSession> findByFacultyEmailOrderByCreatedAtDesc(String facultyEmail);

    List<AttendanceSession> findByCourseNameOrderByCreatedAtDesc(String courseName);

    List<AttendanceSession> findByFacultyEmailAndActiveTrue(String facultyEmail);

    List<AttendanceSession> findByFacultyEmailAndCourseNameAndActiveTrue(String facultyEmail, String courseName);
}
