package com.smartcollege.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.smartcollege.entity.Attendance;
import com.smartcollege.entity.AttendanceSession;
import com.smartcollege.entity.Student;
import com.smartcollege.repository.AttendanceRepository;
import com.smartcollege.repository.AttendanceSessionRepository;
import com.smartcollege.repository.StudentRepository;

@SpringBootTest
@Transactional
class LectureWiseAttendanceTest {

    @Autowired
    private QrAttendanceService qrAttendanceService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private StudentRepository studentRepository;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Create test student enrolled in ACN
        testStudent = new Student();
        testStudent.setName("Alice Sharma");
        testStudent.setEmail("alice.sharma@college.edu");
        testStudent.setCourse("ACN");
        testStudent.setMobile("9876543210");
        testStudent = studentRepository.save(testStudent);
    }

    @Test
    void testLectureWiseAttendanceWorkflow() {
        String today = LocalDate.now().toString();

        // 1. Faculty creates Lecture 12 for ACN on today's date
        AttendanceSession sessionLec12 = qrAttendanceService.createSession(
                "ACN", 12, today, "Prof. Rajesh Verma", "prof.verma@college.edu"
        );

        assertNotNull(sessionLec12.getId());
        assertEquals("ACN", sessionLec12.getCourseName());
        assertEquals(12, sessionLec12.getLectureNumber());
        assertEquals(today, sessionLec12.getLectureDate());
        assertTrue(sessionLec12.isActive());
        assertFalse(sessionLec12.isExpired());

        // 2. Faculty creates Lecture 13 for ACN on the SAME date (separate session)
        AttendanceSession sessionLec13 = qrAttendanceService.createSession(
                "ACN", 13, today, "Prof. Rajesh Verma", "prof.verma@college.edu"
        );

        assertNotNull(sessionLec13.getId());
        assertEquals("ACN", sessionLec13.getCourseName());
        assertEquals(13, sessionLec13.getLectureNumber());
        assertFalse(sessionLec12.getSessionToken().equals(sessionLec13.getSessionToken()));

        // 3. Student scans Lecture 12
        // Temporarily activate sessionLec12 to simulate student scan during lecture 12
        sessionLec12.setActive(true);
        sessionRepository.save(sessionLec12);

        QrAttendanceService.AttendanceResult result1 = qrAttendanceService.submitStudentAttendance(
                sessionLec12.getSessionToken(), testStudent.getEmail()
        );
        assertTrue(result1.isSuccess(), "First scan for Lecture 12 should succeed");

        // 4. Duplicate prevention for same lecture session: Student tries to scan Lecture 12 again
        QrAttendanceService.AttendanceResult duplicateResult = qrAttendanceService.submitStudentAttendance(
                sessionLec12.getSessionToken(), testStudent.getEmail()
        );
        assertFalse(duplicateResult.isSuccess(), "Duplicate scan for same Lecture 12 must be blocked");
        assertTrue(duplicateResult.getMessage().contains("already marked"));

        // 5. Student attends Lecture 13: Student scans Lecture 13 QR code on the SAME date
        QrAttendanceService.AttendanceResult result2 = qrAttendanceService.submitStudentAttendance(
                sessionLec13.getSessionToken(), testStudent.getEmail()
        );
        assertTrue(result2.isSuccess(), "Student should be able to legitimately attend Lecture 13 on the same day");

        // 6. Verify independent attendance records stored in database
        List<Attendance> lec12Attendees = attendanceRepository.findBySessionToken(sessionLec12.getSessionToken());
        assertEquals(1, lec12Attendees.size());
        assertEquals(12, lec12Attendees.get(0).getLectureNumber());
        assertEquals("ACN", lec12Attendees.get(0).getCourseName());
        assertEquals("Present", lec12Attendees.get(0).getStatus());

        List<Attendance> lec13Attendees = attendanceRepository.findBySessionToken(sessionLec13.getSessionToken());
        assertEquals(1, lec13Attendees.size());
        assertEquals(13, lec13Attendees.get(0).getLectureNumber());
        assertEquals("ACN", lec13Attendees.get(0).getCourseName());
        assertEquals("Present", lec13Attendees.get(0).getStatus());

        // 7. Verify student has two independent attendance records for the two lectures
        List<Attendance> studentRecords = attendanceRepository.findByStudentEmail(testStudent.getEmail());
        assertEquals(2, studentRecords.size());
    }

    @Test
    void testManualAttendanceForSpecificLecture() {
        String today = LocalDate.now().toString();

        // Second student who missed the QR scan
        Student bob = new Student();
        bob.setName("Bob Mehta");
        bob.setEmail("bob.mehta@college.edu");
        bob.setCourse("OSY");
        bob = studentRepository.save(bob);

        // Faculty creates OSY Lecture 5
        AttendanceSession sessionOsy = qrAttendanceService.createSession(
                "OSY", 5, today, "Prof. Kulkarni", "prof.kulkarni@college.edu"
        );

        // Faculty manually marks Bob
        QrAttendanceService.AttendanceResult manualRes = qrAttendanceService.markManualAttendance(
                sessionOsy.getSessionToken(), bob.getId(), "prof.kulkarni@college.edu"
        );
        assertTrue(manualRes.isSuccess());

        Attendance recorded = attendanceRepository.findBySessionTokenAndStudentEmail(
                sessionOsy.getSessionToken(), bob.getEmail()
        );
        assertNotNull(recorded);
        assertEquals("MANUAL", recorded.getMarkingType());
        assertEquals(5, recorded.getLectureNumber());
        assertEquals("OSY", recorded.getCourseName());
        assertEquals("Present", recorded.getStatus());
    }
}
