package com.smartcollege.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcollege.entity.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Attendance findByStudentNameAndDate(String studentName, String date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.studentName = :studentName")
    long getTotalAttendanceByStudent(@Param("studentName") String studentName);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.studentName = :studentName AND a.status='Present'")
    long getPresentAttendanceByStudent(@Param("studentName") String studentName);

    boolean existsBySessionTokenAndStudentEmail(String sessionToken, String studentEmail);

    Attendance findBySessionTokenAndStudentEmail(String sessionToken, String studentEmail);

    List<Attendance> findBySessionToken(String sessionToken);

    List<Attendance> findByStudentEmail(String studentEmail);

    List<Attendance> findByCourseNameAndDate(String courseName, String date);
}