package com.smartcollege.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "attendance", uniqueConstraints = {
    @UniqueConstraint(name = "uk_session_student_email", columnNames = {"sessionToken", "studentEmail"})
})
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentName;
    private String courseName;
    private String date;
    private String status;

    // Secure QR Code Attendance & Audit additions
    private String studentEmail;
    private String sessionToken;

    @Column(nullable = false, columnDefinition = "varchar(20) default 'MANUAL'")
    private String markingType = "MANUAL"; // "QR" or "MANUAL"

    private LocalDateTime timestamp;
    private Double latitude;
    private Double longitude;
    private Double distanceMeters;

    public Attendance() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getMarkingType() {
        return markingType;
    }

    public void setMarkingType(String markingType) {
        this.markingType = markingType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
}