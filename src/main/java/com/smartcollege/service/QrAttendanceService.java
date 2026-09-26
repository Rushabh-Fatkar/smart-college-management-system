package com.smartcollege.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcollege.entity.Attendance;
import com.smartcollege.entity.AttendanceSession;
import com.smartcollege.entity.Student;
import com.smartcollege.repository.AttendanceRepository;
import com.smartcollege.repository.AttendanceSessionRepository;
import com.smartcollege.repository.StudentRepository;
import com.smartcollege.util.QrCodeGenerator;

@Service
public class QrAttendanceService {

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Create a new unique attendance session valid for exactly 2 minutes (120 seconds).
     * Deactivates previous active sessions for the same faculty (anti-reuse).
     */
    public AttendanceSession createSession(String courseName, Integer lectureNumber, String lectureDate,
                                           String facultyName, String facultyEmail) {
        // Deactivate previous active sessions for this faculty
        if (facultyEmail != null && !facultyEmail.trim().isEmpty()) {
            List<AttendanceSession> previousActive = sessionRepository.findByFacultyEmailAndActiveTrue(facultyEmail.trim());
            for (AttendanceSession prev : previousActive) {
                prev.setActive(false);
                sessionRepository.save(prev);
            }
        }

        AttendanceSession session = new AttendanceSession();
        // Generate secure random UUID token (no sensitive data)
        String token = UUID.randomUUID().toString().replace("-", "");
        session.setSessionToken(token);
        session.setCourseName(courseName);
        session.setLectureNumber(lectureNumber != null ? lectureNumber : 1);
        session.setLectureDate(lectureDate != null && !lectureDate.trim().isEmpty() ? lectureDate.trim() : LocalDate.now().toString());
        session.setFacultyName(facultyName);
        session.setFacultyEmail(facultyEmail);
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setExpiresAt(now.plusSeconds(120)); // Exactly 2 minutes validity
        session.setActive(true);

        return sessionRepository.save(session);
    }

    /**
     * Overloaded method for backward-compatibility.
     */
    public AttendanceSession createSession(String courseName, String facultyName, String facultyEmail) {
        return createSession(courseName, 1, LocalDate.now().toString(), facultyName, facultyEmail);
    }

    public Optional<AttendanceSession> getSessionByToken(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return Optional.empty();
        }
        return sessionRepository.findBySessionToken(sessionToken.trim());
    }

    public String generateQrCodeBase64(String sessionToken) {
        try {
            return QrCodeGenerator.generateQrCodeBase64(sessionToken, 320, 320);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Process student QR attendance submission with server-side validations:
     * - Student authentication
     * - Valid session token
     * - Active session check
     * - 2-minute expiration check
     * - Course match check
     * - Duplicate attendance prevention (session + student)
     */
    @Transactional
    public AttendanceResult submitStudentAttendance(String sessionToken, String studentEmail) {
        if (studentEmail == null || studentEmail.trim().isEmpty()) {
            return new AttendanceResult(false, "User not authenticated. Please log in.");
        }

        Student student = studentRepository.findByEmail(studentEmail);
        if (student == null) {
            return new AttendanceResult(false, "Student account not found.");
        }

        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return new AttendanceResult(false, "Invalid QR code. Session identifier missing.");
        }

        Optional<AttendanceSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken.trim());
        if (sessionOpt.isEmpty()) {
            return new AttendanceResult(false, "Invalid QR code. Attendance session not found.");
        }

        AttendanceSession session = sessionOpt.get();

        // 1. Active Check (Session closed/deactivated)
        if (!session.isActive()) {
            return new AttendanceResult(false, "This attendance session has been closed. Please scan the latest QR code.");
        }

        // 2. Expiry Check (Server-side 2-minute rule)
        if (session.isExpired()) {
            return new AttendanceResult(false, "QR code expired. Please scan the latest QR code.");
        }

        // 3. Course Match Check (if student is assigned to a specific course)
        if (student.getCourse() != null && !student.getCourse().trim().isEmpty()
                && session.getCourseName() != null && !session.getCourseName().trim().isEmpty()) {
            if (!student.getCourse().trim().equalsIgnoreCase(session.getCourseName().trim())) {
                return new AttendanceResult(false, "You are enrolled in " + student.getCourse()
                        + ", but this attendance session is for " + session.getCourseName() + ".");
            }
        }

        // 4. Duplicate Check
        if (attendanceRepository.existsBySessionTokenAndStudentEmail(session.getSessionToken(), studentEmail)) {
            return new AttendanceResult(false, "Attendance already marked for this lecture session.",
                    session.getCourseName(), session.getLectureNumber(), session.getDisplayDate());
        }

        // 5. Record Attendance
        try {
            Attendance attendance = new Attendance();
            attendance.setStudentName(student.getName());
            attendance.setStudentEmail(student.getEmail());
            attendance.setCourseName(session.getCourseName());
            attendance.setLectureNumber(session.getLectureNumber());
            String recordDate = session.getLectureDate() != null && !session.getLectureDate().trim().isEmpty()
                    ? session.getLectureDate().trim() : LocalDate.now().toString();
            attendance.setDate(recordDate);
            attendance.setLectureDate(recordDate);
            attendance.setStatus("Present");
            attendance.setSessionToken(session.getSessionToken());
            attendance.setMarkingType("QR");
            attendance.setTimestamp(LocalDateTime.now());

            attendanceRepository.save(attendance);
            return new AttendanceResult(true, "Attendance marked successfully! Status: Present.",
                    session.getCourseName(), session.getLectureNumber(), session.getDisplayDate());
        } catch (DataIntegrityViolationException e) {
            return new AttendanceResult(false, "Attendance already marked for this lecture session.",
                    session.getCourseName(), session.getLectureNumber(), session.getDisplayDate());
        } catch (Exception e) {
            return new AttendanceResult(false, "Unable to record attendance: " + e.getMessage());
        }
    }

    /**
     * Mark manual attendance by Faculty for students who could not scan the QR.
     */
    @Transactional
    public AttendanceResult markManualAttendance(String sessionToken, Long studentId, String facultyEmail) {
        Optional<AttendanceSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (sessionOpt.isEmpty()) {
            return new AttendanceResult(false, "Attendance session not found.");
        }

        AttendanceSession session = sessionOpt.get();

        // Verify faculty authorization
        if (facultyEmail != null && session.getFacultyEmail() != null
                && !facultyEmail.equalsIgnoreCase(session.getFacultyEmail())) {
            return new AttendanceResult(false, "Unauthorized: You did not create this attendance session.");
        }

        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return new AttendanceResult(false, "Student not found.");
        }

        Student student = studentOpt.get();

        if (attendanceRepository.existsBySessionTokenAndStudentEmail(sessionToken, student.getEmail())) {
            return new AttendanceResult(false, "Student has already been marked for this session.");
        }

        try {
            Attendance attendance = new Attendance();
            attendance.setStudentName(student.getName());
            attendance.setStudentEmail(student.getEmail());
            attendance.setCourseName(session.getCourseName());
            attendance.setLectureNumber(session.getLectureNumber());
            String recordDate = session.getLectureDate() != null && !session.getLectureDate().trim().isEmpty()
                    ? session.getLectureDate().trim() : LocalDate.now().toString();
            attendance.setDate(recordDate);
            attendance.setLectureDate(recordDate);
            attendance.setStatus("Present");
            attendance.setSessionToken(session.getSessionToken());
            attendance.setMarkingType("MANUAL");
            attendance.setTimestamp(LocalDateTime.now());

            attendanceRepository.save(attendance);
            return new AttendanceResult(true, "Student " + student.getName() + " marked Present manually.");
        } catch (DataIntegrityViolationException e) {
            return new AttendanceResult(false, "Student has already been marked for this session.");
        } catch (Exception e) {
            return new AttendanceResult(false, "Failed to mark manual attendance: " + e.getMessage());
        }
    }

    public List<Attendance> getAttendeesForSession(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return attendanceRepository.findBySessionToken(sessionToken.trim());
    }

    public List<Student> getUnmarkedStudentsForSession(String sessionToken) {
        Optional<AttendanceSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (sessionOpt.isEmpty()) {
            return new ArrayList<>();
        }

        AttendanceSession session = sessionOpt.get();
        List<Attendance> attendees = attendanceRepository.findBySessionToken(sessionToken);
        List<String> markedEmails = attendees.stream()
                .map(Attendance::getStudentEmail)
                .filter(email -> email != null)
                .collect(Collectors.toList());

        List<Student> allStudents = studentRepository.findAll();
        return allStudents.stream()
                .filter(s -> {
                    // Match course if student has course matching session course name, or include all if matching
                    boolean courseMatch = (s.getCourse() != null && s.getCourse().equalsIgnoreCase(session.getCourseName()));
                    return courseMatch && !markedEmails.contains(s.getEmail());
                })
                .collect(Collectors.toList());
    }

    public void deactivateSession(String sessionToken) {
        if (sessionToken != null && !sessionToken.trim().isEmpty()) {
            sessionRepository.findBySessionToken(sessionToken.trim()).ifPresent(s -> {
                s.setActive(false);
                sessionRepository.save(s);
            });
        }
    }

    public List<AttendanceSession> getSessionsByFaculty(String facultyEmail) {
        return sessionRepository.findByFacultyEmailOrderByCreatedAtDesc(facultyEmail);
    }

    public static class AttendanceResult {
        private final boolean success;
        private final String message;
        private final String courseName;
        private final Integer lectureNumber;
        private final String lectureDate;

        public AttendanceResult(boolean success, String message) {
            this(success, message, null, null, null);
        }

        public AttendanceResult(boolean success, String message, String courseName, Integer lectureNumber, String lectureDate) {
            this.success = success;
            this.message = message;
            this.courseName = courseName;
            this.lectureNumber = lectureNumber;
            this.lectureDate = lectureDate;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getCourseName() {
            return courseName;
        }

        public Integer getLectureNumber() {
            return lectureNumber;
        }

        public String getLectureDate() {
            return lectureDate;
        }
    }
}
