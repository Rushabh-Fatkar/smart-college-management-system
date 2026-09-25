package com.smartcollege.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcollege.entity.SemesterMarksSession;
import com.smartcollege.entity.Student;
import com.smartcollege.entity.StudentMarks;
import com.smartcollege.repository.SemesterMarksSessionRepository;
import com.smartcollege.repository.StudentMarksRepository;

@Service
public class SemesterMarksService {

    @Autowired
    private SemesterMarksSessionRepository sessionRepository;

    @Autowired
    private StudentMarksRepository studentMarksRepository;

    // ==================== ADMIN SESSION MANAGEMENT ====================

    public List<SemesterMarksSession> getAllSessions() {
        return sessionRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<SemesterMarksSession> getSessionById(Long id) {
        return sessionRepository.findById(id);
    }

    public Optional<SemesterMarksSession> getActiveOpenSession() {
        return sessionRepository.findFirstByStatusOrderByOpenedAtDesc("OPEN");
    }

    @Transactional
    public SemesterMarksSession createSession(String sessionName, String semester, String academicYear, boolean openImmediately) {
        SemesterMarksSession session = new SemesterMarksSession();
        session.setSessionName(sessionName);
        session.setSemester(semester);
        session.setAcademicYear(academicYear);
        session.setCreatedAt(LocalDateTime.now());

        if (openImmediately) {
            session.setStatus("OPEN");
            session.setOpenedAt(LocalDateTime.now());
        } else {
            session.setStatus("CLOSED");
        }

        return sessionRepository.save(session);
    }

    @Transactional
    public SemesterMarksSession toggleSessionStatus(Long sessionId) {
        SemesterMarksSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with id: " + sessionId));

        if ("OPEN".equalsIgnoreCase(session.getStatus())) {
            session.setStatus("CLOSED");
            session.setClosedAt(LocalDateTime.now());
        } else {
            session.setStatus("OPEN");
            session.setOpenedAt(LocalDateTime.now());
            session.setClosedAt(null);
        }

        return sessionRepository.save(session);
    }

    public List<StudentMarks> getSubmissionsForSession(Long sessionId) {
        return studentMarksRepository.findByMarksSessionId(sessionId);
    }

    public Optional<StudentMarks> getMarkById(Long id) {
        return studentMarksRepository.findById(id);
    }

    /**
     * Admin correction function: Allows Admin to edit and correct submitted student marks.
     */
    @Transactional
    public MarksOperationResult updateStudentMarkByAdmin(Long markId, Double marksObtained, Double totalMarks, String adminRemarks) {
        Optional<StudentMarks> markOpt = studentMarksRepository.findById(markId);
        if (markOpt.isEmpty()) {
            return new MarksOperationResult(false, "Mark record not found.");
        }

        StudentMarks mark = markOpt.get();

        if (marksObtained == null || marksObtained < 0) {
            return new MarksOperationResult(false, "Marks obtained cannot be negative.");
        }

        if (totalMarks == null || totalMarks <= 0) {
            return new MarksOperationResult(false, "Total marks must be greater than zero.");
        }

        if (marksObtained > totalMarks) {
            return new MarksOperationResult(false, "Marks obtained cannot exceed total marks.");
        }

        mark.setMarksObtained(marksObtained);
        mark.setTotalMarks(totalMarks);
        mark.calculateGrade();
        mark.setLastModifiedTimestamp(LocalDateTime.now());

        String remarkText = "Corrected by Admin on " + LocalDateTime.now().toLocalDate();
        if (adminRemarks != null && !adminRemarks.trim().isEmpty()) {
            remarkText += " (" + adminRemarks.trim() + ")";
        }
        mark.setRemarks(remarkText);

        studentMarksRepository.save(mark);
        return new MarksOperationResult(true, "Student mark successfully updated by Admin.");
    }

    // ==================== STUDENT MARKS SUBMISSION ====================

    public List<StudentMarks> getStudentMarksForSession(Student student, SemesterMarksSession session) {
        if (student == null || session == null) {
            return new ArrayList<>();
        }
        return studentMarksRepository.findByStudentAndMarksSession(student, session);
    }

    public boolean hasStudentSubmittedForSession(Student student, SemesterMarksSession session) {
        if (student == null || session == null) {
            return false;
        }
        return studentMarksRepository.existsByStudentAndMarksSession(student, session);
    }

    /**
     * Final submission of student marks:
     * - Backend verification that session is OPEN.
     * - Backend verification that student hasn't already submitted (enforcing lock).
     * - Backend validation of marks ranges.
     * - Marks locked permanently for the student.
     */
    @Transactional
    public MarksOperationResult submitStudentMarks(Student student, Long sessionId, List<SubjectMarkDTO> entries) {
        if (student == null) {
            return new MarksOperationResult(false, "Student account not found.");
        }

        Optional<SemesterMarksSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return new MarksOperationResult(false, "Semester marks session not found.");
        }

        SemesterMarksSession session = sessionOpt.get();

        // 1. Enforce session is OPEN on backend
        if (!session.isOpen()) {
            return new MarksOperationResult(false, "Semester marks submission session is currently closed.");
        }

        // 2. Enforce lock: Student cannot edit after final submission
        if (studentMarksRepository.existsByStudentAndMarksSession(student, session)) {
            return new MarksOperationResult(false, "Marks have already been submitted and locked for this session.");
        }

        if (entries == null || entries.isEmpty()) {
            return new MarksOperationResult(false, "Please provide marks for at least one subject.");
        }

        LocalDateTime now = LocalDateTime.now();
        List<StudentMarks> toSave = new ArrayList<>();

        for (SubjectMarkDTO entry : entries) {
            if (entry.getSubjectName() == null || entry.getSubjectName().trim().isEmpty()) {
                continue;
            }

            if (entry.getMarksObtained() == null || entry.getMarksObtained() < 0) {
                return new MarksOperationResult(false, "Marks obtained cannot be negative for subject: " + entry.getSubjectName());
            }

            double total = (entry.getTotalMarks() != null && entry.getTotalMarks() > 0) ? entry.getTotalMarks() : 100.0;
            if (entry.getMarksObtained() > total) {
                return new MarksOperationResult(false, "Marks obtained cannot exceed total marks for subject: " + entry.getSubjectName());
            }

            StudentMarks mark = new StudentMarks();
            mark.setStudent(student);
            mark.setMarksSession(session);
            mark.setCourseName(student.getCourse());
            mark.setSubjectName(entry.getSubjectName().trim());
            mark.setMarksObtained(entry.getMarksObtained());
            mark.setTotalMarks(total);
            mark.calculateGrade();
            mark.setStatus("SUBMITTED"); // LOCKED
            mark.setSubmissionTimestamp(now);
            mark.setRemarks("Submitted by Student");

            toSave.add(mark);
        }

        if (toSave.isEmpty()) {
            return new MarksOperationResult(false, "No valid subject marks to submit.");
        }

        try {
            studentMarksRepository.saveAll(toSave);
            return new MarksOperationResult(true, "Marks submitted successfully and locked.");
        } catch (DataIntegrityViolationException e) {
            return new MarksOperationResult(false, "Duplicate marks entry detected for the same subject in this session.");
        } catch (Exception e) {
            return new MarksOperationResult(false, "Failed to submit marks: " + e.getMessage());
        }
    }

    public static class SubjectMarkDTO {
        private String subjectName;
        private Double marksObtained;
        private Double totalMarks = 100.0;

        public SubjectMarkDTO() {
        }

        public SubjectMarkDTO(String subjectName, Double marksObtained, Double totalMarks) {
            this.subjectName = subjectName;
            this.marksObtained = marksObtained;
            this.totalMarks = totalMarks;
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
        }

        public Double getTotalMarks() {
            return totalMarks;
        }

        public void setTotalMarks(Double totalMarks) {
            this.totalMarks = totalMarks;
        }
    }

    public static class MarksOperationResult {
        private final boolean success;
        private final String message;

        public MarksOperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
