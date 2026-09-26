package com.smartcollege.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_session", indexes = {
    @jakarta.persistence.Index(name = "idx_att_sess_course_lec_date", columnList = "courseName, lectureNumber, lectureDate")
})
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sessionToken;

    @Column(nullable = false)
    private String courseName;

    @Column(name = "lecture_number")
    private Integer lectureNumber;

    @Column(name = "lecture_date", length = 32)
    private String lectureDate;

    private String facultyName;
    private String facultyEmail;


    @Column(nullable = false)
    private LocalDateTime createdAt;

    // QR validity is exactly 2 minutes (120 seconds) from generation time
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private boolean active = true;

    public AttendanceSession() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getFacultyEmail() {
        return facultyEmail;
    }

    public void setFacultyEmail(String facultyEmail) {
        this.facultyEmail = facultyEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public Integer getLectureNumber() {
        return lectureNumber;
    }

    public void setLectureNumber(Integer lectureNumber) {
        this.lectureNumber = lectureNumber;
    }

    public String getLectureDate() {
        return lectureDate;
    }

    public void setLectureDate(String lectureDate) {
        this.lectureDate = lectureDate;
    }

    public String getDisplayDate() {
        if (lectureDate != null && lectureDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = lectureDate.split("-");
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }
        return lectureDate != null ? lectureDate : "";
    }
}
