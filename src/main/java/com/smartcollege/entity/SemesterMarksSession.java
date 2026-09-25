package com.smartcollege.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "semester_marks_session")
public class SemesterMarksSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionName; // e.g. "Fall 2026 - Semester 1"

    @Column(nullable = false)
    private String semester; // e.g. "Semester 1", "Semester 2", ...

    private String academicYear; // e.g. "2025-2026"

    @Column(nullable = false)
    private String status = "CLOSED"; // "OPEN" or "CLOSED"

    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SemesterMarksSession() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(this.status);
    }
}
