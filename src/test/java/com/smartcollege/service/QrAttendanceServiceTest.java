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

    @Test
    void testLectureWiseAttendanceSessionCreation() {
        AttendanceSession session = new AttendanceSession();
        session.setCourseName("ACN");
        session.setLectureNumber(12);
        session.setLectureDate("2026-09-26");

        assertEquals("ACN", session.getCourseName());
        assertEquals(12, session.getLectureNumber());
        assertEquals("2026-09-26", session.getLectureDate());
        assertEquals("26-09-2026", session.getDisplayDate());
    }

    @Test
    void testLectureWiseAttendanceDisplayDateFormatting() {
        com.smartcollege.entity.Attendance att = new com.smartcollege.entity.Attendance();
        att.setCourseName("ACN");
        att.setLectureNumber(12);
        att.setDate("2026-09-26");
        att.setLectureDate("2026-09-26");
        att.setStatus("Present");

        assertEquals("26-09-2026", att.getDisplayDate());
        assertEquals(12, att.getLectureNumber());
        assertEquals("ACN", att.getCourseName());
        assertEquals("Present", att.getStatus());

        // Already formatted DD-MM-YYYY
        com.smartcollege.entity.Attendance att2 = new com.smartcollege.entity.Attendance();
        att2.setDate("26-09-2026");
        assertEquals("26-09-2026", att2.getDisplayDate());
    }

    @Test
    void testSeparateLectureSessionsNotMerged() {
        // Lecture 12 session
        AttendanceSession session12 = new AttendanceSession();
        session12.setCourseName("ACN");
        session12.setLectureNumber(12);
        session12.setLectureDate("2026-09-26");
        session12.setSessionToken("token_acn_12");

        // Lecture 13 session
        AttendanceSession session13 = new AttendanceSession();
        session13.setCourseName("ACN");
        session13.setLectureNumber(13);
        session13.setLectureDate("2026-09-26");
        session13.setSessionToken("token_acn_13");

        // OSY Lecture 5 session
        AttendanceSession sessionOsy = new AttendanceSession();
        sessionOsy.setCourseName("OSY");
        sessionOsy.setLectureNumber(5);
        sessionOsy.setLectureDate("2026-09-26");
        sessionOsy.setSessionToken("token_osy_5");

        // Assert distinct session tokens and lecture numbers
        assertFalse(session12.getSessionToken().equals(session13.getSessionToken()));
        assertFalse(session12.getSessionToken().equals(sessionOsy.getSessionToken()));
        assertEquals(12, session12.getLectureNumber());
        assertEquals(13, session13.getLectureNumber());
        assertEquals(5, sessionOsy.getLectureNumber());
    }

    @Test
    void testAttendanceResultLectureDetails() {
        QrAttendanceService.AttendanceResult result = new QrAttendanceService.AttendanceResult(
                true, "Attendance marked successfully! Status: Present.", "ACN", 12, "26-09-2026"
        );
        assertTrue(result.isSuccess());
        assertEquals("Attendance marked successfully! Status: Present.", result.getMessage());
        assertEquals("ACN", result.getCourseName());
        assertEquals(12, result.getLectureNumber());
        assertEquals("26-09-2026", result.getLectureDate());
    }
}
