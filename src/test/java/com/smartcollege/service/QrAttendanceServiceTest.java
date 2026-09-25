package com.smartcollege.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.smartcollege.entity.AttendanceSession;
import com.smartcollege.entity.StudentMarks;

class QrAttendanceServiceTest {

    @Test
    void testHaversineDistanceCalculation() {
        // Point A: New York (approx 40.7128, -74.0060)
        // Point B: Very close point ~111 meters away (40.7138, -74.0060)
        double distance = QrAttendanceService.calculateDistanceMeters(40.7128, -74.0060, 40.7138, -74.0060);
        assertTrue(distance > 100 && distance < 120, "Distance should be around 111 meters, got: " + distance);

        // Same point distance should be 0
        double zeroDist = QrAttendanceService.calculateDistanceMeters(18.5204, 73.8567, 18.5204, 73.8567);
        assertEquals(0.0, zeroDist, 0.001);
    }

    @Test
    void testAttendanceSessionExpiry() {
        AttendanceSession session = new AttendanceSession();
        session.setCreatedAt(LocalDateTime.now().minusSeconds(125));
        session.setExpiresAt(LocalDateTime.now().minusSeconds(5));

        assertTrue(session.isExpired(), "Session older than 120 seconds should be expired");

        AttendanceSession activeSession = new AttendanceSession();
        activeSession.setCreatedAt(LocalDateTime.now());
        activeSession.setExpiresAt(LocalDateTime.now().plusSeconds(120));

        assertFalse(activeSession.isExpired(), "Fresh session should not be expired");
    }

    @Test
    void testStudentMarksGradeCalculation() {
        StudentMarks mark = new StudentMarks();
        mark.setTotalMarks(100.0);

        mark.setMarksObtained(95.0);
        assertEquals("A+", mark.getGrade());

        mark.setMarksObtained(85.0);
        assertEquals("A", mark.getGrade());

        mark.setMarksObtained(75.0);
        assertEquals("B", mark.getGrade());

        mark.setMarksObtained(65.0);
        assertEquals("C", mark.getGrade());

        mark.setMarksObtained(55.0);
        assertEquals("D", mark.getGrade());

        mark.setMarksObtained(45.0);
        assertEquals("E", mark.getGrade());

        mark.setMarksObtained(30.0);
        assertEquals("F", mark.getGrade());
    }

    @Test
    void testSemesterMarksSessionStatus() {
        com.smartcollege.entity.SemesterMarksSession session = new com.smartcollege.entity.SemesterMarksSession();
        session.setStatus("OPEN");
        assertTrue(session.isOpen(), "Session with status OPEN should return true for isOpen()");

        session.setStatus("CLOSED");
        assertFalse(session.isOpen(), "Session with status CLOSED should return false for isOpen()");

        session.setStatus(null);
        assertFalse(session.isOpen(), "Session with status null should return false for isOpen()");
    }

    @Test
    void testStudentMarksCustomTotalGradeCalculation() {
        StudentMarks mark = new StudentMarks();
        // 45 out of 50 = 90% -> A+
        mark.setTotalMarks(50.0);
        mark.setMarksObtained(45.0);
        mark.calculateGrade();
        assertEquals("A+", mark.getGrade());

        // 35 out of 50 = 70% -> B
        mark.setMarksObtained(35.0);
        mark.calculateGrade();
        assertEquals("B", mark.getGrade());

        // 15 out of 50 = 30% -> F
        mark.setMarksObtained(15.0);
        mark.calculateGrade();
        assertEquals("F", mark.getGrade());
    }
}
