package com.smartcollege.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartcollege.entity.SemesterMarksSession;
import com.smartcollege.entity.Student;
import com.smartcollege.entity.StudentMarks;
import com.smartcollege.service.CourseService;
import com.smartcollege.service.SemesterMarksService;
import com.smartcollege.service.SemesterMarksService.SubjectMarkDTO;
import com.smartcollege.service.StudentService;

@Controller
public class SemesterMarksController {

    @Autowired
    private SemesterMarksService semesterMarksService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Admin: View all semester marks sessions & create new session.
     */
    @GetMapping("/admin/marks-sessions")
    public String viewMarksSessions(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        List<SemesterMarksSession> sessions = semesterMarksService.getAllSessions();
        model.addAttribute("sessions", sessions);
        return "admin-marks-sessions";
    }

    /**
     * Admin: Create and optionally open a new marks submission session.
     */
    @PostMapping("/admin/marks-sessions/create")
    public String createMarksSession(@RequestParam String sessionName,
                                     @RequestParam String semester,
                                     @RequestParam(required = false) String academicYear,
                                     @RequestParam(defaultValue = "true") boolean openImmediately,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        if (sessionName == null || sessionName.trim().isEmpty() || semester == null || semester.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Session name and semester are required.");
            return "redirect:/admin/marks-sessions";
        }

        semesterMarksService.createSession(
                sessionName.trim(),
                semester.trim(),
                academicYear != null ? academicYear.trim() : "",
                openImmediately
        );

        redirectAttributes.addFlashAttribute("success", "Semester marks session created successfully.");
        return "redirect:/admin/marks-sessions";
    }

    /**
     * Admin: Toggle session status (OPEN / CLOSE).
     */
    @PostMapping("/admin/marks-sessions/{id}/toggle-status")
    public String toggleSessionStatus(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        try {
            SemesterMarksSession updated = semesterMarksService.toggleSessionStatus(id);
            redirectAttributes.addFlashAttribute("success",
                    "Session '" + updated.getSessionName() + "' is now " + updated.getStatus() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update session: " + e.getMessage());
        }

        return "redirect:/admin/marks-sessions";
    }

    /**
     * Admin: View all submitted student marks for a given session.
     */
    @GetMapping("/admin/marks-sessions/{id}/submissions")
    public String viewSessionSubmissions(@PathVariable Long id, Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        Optional<SemesterMarksSession> sessionOpt = semesterMarksService.getSessionById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/admin/marks-sessions";
        }

        SemesterMarksSession marksSession = sessionOpt.get();
        List<StudentMarks> submissions = semesterMarksService.getSubmissionsForSession(id);

        model.addAttribute("marksSession", marksSession);
        model.addAttribute("submissions", submissions);
        return "admin-marks-submissions";
    }

    /**
     * Admin: Edit/Correct a specific student mark.
     */
    @GetMapping("/admin/marks/edit/{id}")
    public String showEditMarkPage(@PathVariable Long id, Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        Optional<StudentMarks> markOpt = semesterMarksService.getMarkById(id);
        if (markOpt.isEmpty()) {
            return "redirect:/admin/marks-sessions";
        }

        model.addAttribute("mark", markOpt.get());
        return "admin-marks-edit";
    }

    /**
     * Admin: Save edited student mark.
     */
    @PostMapping("/admin/marks/edit/{id}")
    public String updateStudentMark(@PathVariable Long id,
                                    @RequestParam Double marksObtained,
                                    @RequestParam Double totalMarks,
                                    @RequestParam(required = false) String remarks,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        SemesterMarksService.MarksOperationResult result =
                semesterMarksService.updateStudentMarkByAdmin(id, marksObtained, totalMarks, remarks);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
            Optional<StudentMarks> markOpt = semesterMarksService.getMarkById(id);
            if (markOpt.isPresent()) {
                return "redirect:/admin/marks-sessions/" + markOpt.get().getMarksSession().getId() + "/submissions";
            }
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
            return "redirect:/admin/marks/edit/" + id;
        }

        return "redirect:/admin/marks-sessions";
    }

    // ==================== STUDENT ENDPOINTS ====================

    /**
     * Student: View Semester Marks section.
     * - If session is OPEN and student has NOT submitted: show entry form.
     * - If student has ALREADY submitted: show locked marks table ("Marks submitted successfully and locked.").
     * - If NO session is OPEN: show message that session is currently closed.
     */
    @GetMapping("/student/marks")
    public String showStudentMarksPage(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("userEmail");
        Student student = studentService.findByEmail(email);
        if (student == null) {
            return "redirect:/login";
        }

        model.addAttribute("student", student);

        Optional<SemesterMarksSession> openSessionOpt = semesterMarksService.getActiveOpenSession();
        model.addAttribute("openSession", openSessionOpt.orElse(null));

        if (openSessionOpt.isPresent()) {
            SemesterMarksSession activeSession = openSessionOpt.get();
            boolean isLocked = semesterMarksService.hasStudentSubmittedForSession(student, activeSession);
            model.addAttribute("isLocked", isLocked);

            if (isLocked) {
                List<StudentMarks> submittedMarks = semesterMarksService.getStudentMarksForSession(student, activeSession);
                model.addAttribute("submittedMarks", submittedMarks);
                model.addAttribute("statusMessage", "Marks submitted successfully and locked.");
            }
        } else {
            model.addAttribute("isLocked", false);
            model.addAttribute("sessionClosed", true);
        }

        return "student-marks";
    }

    /**
     * Student: Final submission of marks.
     * Server strictly enforces lock and active session.
     */
    @PostMapping("/student/submit-marks")
    public String submitStudentMarks(@RequestParam Long sessionId,
                                     @RequestParam List<String> subjectNames,
                                     @RequestParam List<Double> marksObtained,
                                     @RequestParam List<Double> totalMarks,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("userEmail");
        Student student = studentService.findByEmail(email);
        if (student == null) {
            return "redirect:/login";
        }

        List<SubjectMarkDTO> entries = new ArrayList<>();
        for (int i = 0; i < subjectNames.size(); i++) {
            String name = subjectNames.get(i);
            Double obtained = (i < marksObtained.size()) ? marksObtained.get(i) : null;
            Double total = (i < totalMarks.size()) ? totalMarks.get(i) : 100.0;
            if (name != null && !name.trim().isEmpty() && obtained != null) {
                entries.add(new SubjectMarkDTO(name.trim(), obtained, total));
            }
        }

        SemesterMarksService.MarksOperationResult result =
                semesterMarksService.submitStudentMarks(student, sessionId, entries);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
        }

        return "redirect:/student/marks";
    }
}
