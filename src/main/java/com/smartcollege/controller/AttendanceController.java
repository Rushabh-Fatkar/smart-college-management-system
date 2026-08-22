package com.smartcollege.controller;

import java.time.LocalDate;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.smartcollege.entity.Attendance;
import com.smartcollege.entity.Student;
import com.smartcollege.service.AttendanceService;
import com.smartcollege.service.StudentService;

@Controller
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private StudentService studentService;

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
                attendanceService.getAllAttendance()
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

        model.addAttribute(
                "attendance",
                new Attendance()
        );

        model.addAttribute(
                "students",
                studentService.getAllStudents()
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
            attendance.setCourseName(
                    student.getCourse()
            );
        }

        attendance.setDate(
                LocalDate.now().toString()
        );

        Attendance existingAttendance =
                attendanceService.findByStudentNameAndDate(
                        attendance.getStudentName(),
                        attendance.getDate()
                );

        if (existingAttendance != null) {

            model.addAttribute(
                    "attendance",
                    attendance
            );

            model.addAttribute(
                    "students",
                    studentService.getAllStudents()
            );

            model.addAttribute(
                    "error",
                    "Attendance already marked for today!"
            );

            return "attendance-register";
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
            Model model,
            HttpSession session) {

        String role =
                (String) session.getAttribute("role");

        if (role == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "attendanceList",
                attendanceService.getAllAttendance()
        );

        if (!attendanceService.getAllAttendance().isEmpty()) {

            String studentName =
                    attendanceService.getAllAttendance()
                            .get(0)
                            .getStudentName();

            long totalAttendance =
                    attendanceService
                            .getTotalAttendanceByStudent(
                                    studentName
                            );

            long presentAttendance =
                    attendanceService
                            .getPresentAttendanceByStudent(
                                    studentName
                            );

            double attendancePercentage = 0;

            if (totalAttendance > 0) {

                attendancePercentage =
                        (presentAttendance * 100.0)
                                / totalAttendance;
            }

            model.addAttribute(
                    "studentName",
                    studentName
            );

            model.addAttribute(
                    "totalAttendance",
                    totalAttendance
            );

            model.addAttribute(
                    "presentAttendance",
                    presentAttendance
            );

            model.addAttribute(
                    "attendancePercentage",
                    String.format(
                            "%.2f",
                            attendancePercentage
                    )
            );

        } else {

            model.addAttribute(
                    "studentName",
                    "No Student"
            );

            model.addAttribute(
                    "totalAttendance",
                    0
            );

            model.addAttribute(
                    "presentAttendance",
                    0
            );

            model.addAttribute(
                    "attendancePercentage",
                    "0.00"
            );
        }

        return "student-attendance";
    }
}