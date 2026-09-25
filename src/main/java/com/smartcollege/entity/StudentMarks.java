package com.smartcollege.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "student_marks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_student_session_subject", columnNames = {"student_id", "session_id", "subjectName"})
})
public class StudentMarks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", nullable = false)
    private SemesterMarksSession marksSession;

    private String courseName;

    @Column(nullable = false)
    private String subjectName;

    @Column(nullable = false)
    private Double marksObtained;

    @Column(nullable = false)
    private Double totalMarks = 100.0;

    private String grade;

    @Column(nullable = false)
    private String status = "SUBMITTED"; // "SUBMITTED" (locked)

    @Column(nullable = false)
    private LocalDateTime submissionTimestamp;

    private LocalDateTime lastModifiedTimestamp;

    private String remarks; // e.g. "Submitted by Student", "Corrected by Admin on ..."

    public StudentMarks() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public SemesterMarksSession getMarksSession() {
        return marksSession;
    }

    public void setMarksSession(SemesterMarksSession marksSession) {
        this.marksSession = marksSession;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Double getMarksObtained() {
        return marksObtained;
    }

    public void setMarksObtained(Double marksObtained) {
        this.marksObtained = marksObtained;
        calculateGrade();
    }

    public Double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Double totalMarks) {
        this.totalMarks = totalMarks;
        calculateGrade();
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubmissionTimestamp() {
        return submissionTimestamp;
    }

    public void setSubmissionTimestamp(LocalDateTime submissionTimestamp) {
        this.submissionTimestamp = submissionTimestamp;
    }

    public LocalDateTime getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(LocalDateTime lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public void calculateGrade() {
        if (totalMarks != null && totalMarks > 0 && marksObtained != null) {
            double percentage = (marksObtained / totalMarks) * 100.0;
            if (percentage >= 90) {
                this.grade = "A+";
            } else if (percentage >= 80) {
                this.grade = "A";
            } else if (percentage >= 70) {
                this.grade = "B";
            } else if (percentage >= 60) {
                this.grade = "C";
            } else if (percentage >= 50) {
                this.grade = "D";
            } else if (percentage >= 40) {
                this.grade = "E";
            } else {
                this.grade = "F";
            }
        }
    }
}
