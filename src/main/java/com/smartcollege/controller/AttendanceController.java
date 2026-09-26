package com.smartcollege.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smartcollege.entity.Attendance;
import com.smartcollege.entity.AttendanceSession;
import com.smartcollege.entity.Student;
import com.smartcollege.repository.AttendanceSessionRepository;
import com.smartcollege.service.AttendanceService;
import com.smartcollege.service.CourseService;
import com.smartcollege.service.StudentService;

@Controller
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @GetMapping("/attendance")
    public String viewAttendance(
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (role == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "attendanceList",
                attendanceService.getAllAttendanceOrdered()
        );

        return "attendance";
    }

    @GetMapping("/attendance-register")
    public String attendanceRegister(
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/student-attendance";
        }

        Attendance attendance = new Attendance();
        attendance.setDate(LocalDate.now().toString());
        attendance.setLectureNumber(1);

        model.addAttribute(
                "attendance",
                attendance
        );

        model.addAttribute(
                "students",
                studentService.getAllStudents()
        );

        model.addAttribute(
                "courses",
                courseService.getAllCourses()
        );

        return "attendance-register";
    }

    @PostMapping("/saveAttendance")
    public String saveAttendance(
            @ModelAttribute Attendance attendance,
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/student-attendance";
        }

        Student student =
                studentService.findByName(
                        attendance.getStudentName()
                );

        if (student != null) {
            if (attendance.getCourseName() == null || attendance.getCourseName().trim().isEmpty()) {
                attendance.setCourseName(student.getCourse());
            }
            if (attendance.getStudentEmail() == null || attendance.getStudentEmail().trim().isEmpty()) {
                attendance.setStudentEmail(student.getEmail());
            }
        }

        if (attendance.getLectureNumber() == null || attendance.getLectureNumber() < 1) {
            attendance.setLectureNumber(1);
        }

        if (attendance.getDate() == null || attendance.getDate().trim().isEmpty()) {
            attendance.setDate(LocalDate.now().toString());
        }
        attendance.setLectureDate(attendance.getDate());

        if (attendance.getSessionToken() == null || attendance.getSessionToken().trim().isEmpty()) {
            attendance.setSessionToken("MANUAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }

        if (attendance.getMarkingType() == null || attendance.getMarkingType().trim().isEmpty()) {
            attendance.setMarkingType("MANUAL");
        }

        if (attendance.getTimestamp() == null) {
            attendance.setTimestamp(LocalDateTime.now());
        }

        // Duplicate check per lecture: prevent marking duplicate for the SAME lecture session
        // while allowing the same student to attend different lectures on the same date!
        if (attendance.getId() == null) {
            boolean alreadyMarked = attendanceService.isAlreadyMarkedForLecture(
                    attendance.getStudentName(),
                    attendance.getStudentEmail(),
                    attendance.getCourseName(),
                    attendance.getLectureNumber(),
                    attendance.getDate()
            );

            if (alreadyMarked) {
                model.addAttribute("attendance", attendance);
                model.addAttribute("students", studentService.getAllStudents());
                model.addAttribute("courses", courseService.getAllCourses());
                model.addAttribute(
                        "error",
                        "Attendance already marked for " + attendance.getStudentName()
                                + " for " + attendance.getCourseName()
                                + " Lecture " + attendance.getLectureNumber()
                                + " on " + attendance.getDisplayDate() + "!"
                );
                return "attendance-register";
            }
        }

        attendanceService.saveAttendance(attendance);

        return "redirect:/attendance";
    }

    @GetMapping("/editAttendance/{id}")
    public String editAttendance(
            @PathVariable Long id,
            Model model,
            HttpSession session) {

        String role =
                (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/student-attendance";
        }

        model.addAttribute(
                "attendance",
                attendanceService.getAttendanceById(id)
        );

        model.addAttribute(
                "students",
                studentService.getAllStudents()
        );

        model.addAttribute(
                "courses",
                courseService.getAllCourses()
        );

        return "attendance-register";
    }

    @GetMapping("/deleteAttendance/{id}")
    public String deleteAttendance(
            @PathVariable Long id,
            HttpSession session) {

        String role =
                (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/student-attendance";
        }

        attendanceService.deleteAttendance(id);

        return "redirect:/attendance";
    }

    @GetMapping("/student-attendance")
    public String studentAttendance(
            @RequestParam(required = false) Long studentId,
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (role == null) {
            return "redirect:/login";
        }

        Student student = null;
        if ("STUDENT".equals(role)) {
            String email = (String) session.getAttribute("userEmail");
            String username = (String) session.getAttribute("username");
            if (email != null && !email.trim().isEmpty()) {
                student = studentService.findByEmail(email.trim());
            }
            if (student == null && username != null && !username.trim().isEmpty()) {
                student = studentService.findByName(username.trim());
            }
        } else {
            // ADMIN or FACULTY viewing
            if (studentId != null) {
                student = studentService.getStudentById(studentId);
            }
            if (student == null) {
                List<Student> all = studentService.getAllStudents();
                if (all != null && !all.isEmpty()) {
                    student = all.get(0);
                }
            }
            model.addAttribute("allStudents", studentService.getAllStudents());
        }

        if (student == null) {
            model.addAttribute("studentName", "No Student");
            model.addAttribute("attendanceList", Collections.emptyList());
            model.addAttribute("totalAttendance", 0);
            model.addAttribute("presentAttendance", 0);
            model.addAttribute("absentAttendance", 0);
            model.addAttribute("attendancePercentage", "0.00");
            return "student-attendance";
        }

        String studentName = student.getName();
        String studentEmail = student.getEmail();
        String studentCourse = student.getCourse();

        // 1. Gather all direct attendance records for this student
        List<Attendance> recordedList = new ArrayList<>();
        if (studentEmail != null && !studentEmail.trim().isEmpty()) {
            recordedList.addAll(attendanceService.getAttendanceByStudentEmail(studentEmail.trim()));
        }
        if (recordedList.isEmpty() && studentName != null) {
            recordedList.addAll(attendanceService.getAttendanceByStudentName(studentName.trim()));
        }

        // Track what sessions or lecture slots the student already has recorded
        Set<String> markedSessionTokens = new HashSet<>();
        Set<String> markedLectureKeys = new HashSet<>();
        for (Attendance a : recordedList) {
            if (a.getSessionToken() != null && !a.getSessionToken().trim().isEmpty()) {
                markedSessionTokens.add(a.getSessionToken().trim());
            }
            if (a.getCourseName() != null && a.getLectureNumber() != null && a.getDate() != null) {
                markedLectureKeys.add(a.getCourseName().trim().toLowerCase() + "_" + a.getLectureNumber() + "_" + a.getDate().trim());
            }
        }

        // 2. Discover sessions conducted for this student's course where student wasn't marked (treated as Absent)
        List<Attendance> fullList = new ArrayList<>(recordedList);
        if (studentCourse != null && !studentCourse.trim().isEmpty()) {
            List<AttendanceSession> courseSessions = sessionRepository.findByCourseNameOrderByCreatedAtDesc(studentCourse.trim());
            for (AttendanceSession s : courseSessions) {
                String token = s.getSessionToken();
                String lecDate = s.getLectureDate() != null && !s.getLectureDate().trim().isEmpty()
                        ? s.getLectureDate().trim()
                        : (s.getCreatedAt() != null ? s.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString());
                String lecKey = s.getCourseName().trim().toLowerCase() + "_" + s.getLectureNumber() + "_" + lecDate;

                // If not marked for this session/lecture and session has expired or deactivated
                if (!markedSessionTokens.contains(token) && !markedLectureKeys.contains(lecKey) && (s.isExpired() || !s.isActive())) {
                    Attendance absentRecord = new Attendance();
                    absentRecord.setStudentName(studentName);
                    absentRecord.setStudentEmail(studentEmail);
                    absentRecord.setCourseName(s.getCourseName());
                    absentRecord.setLectureNumber(s.getLectureNumber());
                    absentRecord.setDate(lecDate);
                    absentRecord.setLectureDate(lecDate);
                    absentRecord.setStatus("Absent");
                    absentRecord.setMarkingType("SYSTEM");
                    absentRecord.setSessionToken(token);
                    absentRecord.setTimestamp(s.getCreatedAt());
                    fullList.add(absentRecord);
                    markedLectureKeys.add(lecKey);
                }
            }
        }

        // 3. Sort organized lecture-wise and date-wise (date DESC, lectureNumber DESC)
        fullList.sort((a, b) -> {
            String dateA = a.getDate() != null ? a.getDate() : "";
            String dateB = b.getDate() != null ? b.getDate() : "";
            int cmpDate = dateB.compareTo(dateA);
            if (cmpDate != 0) return cmpDate;

            int lecA = a.getLectureNumber() != null ? a.getLectureNumber() : 0;
            int lecB = b.getLectureNumber() != null ? b.getLectureNumber() : 0;
            return Integer.compare(lecB, lecA);
        });

        long totalAttendance = fullList.size();
        long presentAttendance = fullList.stream().filter(a -> "Present".equalsIgnoreCase(a.getStatus())).count();
        long absentAttendance = totalAttendance - presentAttendance;

        double attendancePercentage = 0;
        if (totalAttendance > 0) {
            attendancePercentage = (presentAttendance * 100.0) / totalAttendance;
        }

        model.addAttribute("student", student);
        model.addAttribute("studentName", studentName);
        model.addAttribute("studentEmail", studentEmail);
        model.addAttribute("attendanceList", fullList);
        model.addAttribute("totalAttendance", totalAttendance);
        model.addAttribute("presentAttendance", presentAttendance);
        model.addAttribute("absentAttendance", absentAttendance);
        model.addAttribute("attendancePercentage", String.format("%.2f", attendancePercentage));

        return "student-attendance";
    }
}