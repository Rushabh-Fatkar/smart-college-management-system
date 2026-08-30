package com.smartcollege.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.smartcollege.entity.Student;
import com.smartcollege.service.AttendanceService;
import com.smartcollege.service.StudentService;

@Controller
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("/students")
    public String viewStudents(
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (role == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "students",
                studentService.getAllStudents()
        );

        Map<String, Double> attendancePercentage =
                new HashMap<>();

        for (Student student : studentService.getAllStudents()) {

            long total =
                    attendanceService.getTotalAttendanceByStudent(
                            student.getName()
                    );

            long present =
                    attendanceService.getPresentAttendanceByStudent(
                            student.getName()
                    );

            double percentage = 0;

            if (total > 0) {
                percentage =
                        (present * 100.0) / total;
            }

            attendancePercentage.put(
                    student.getName(),
                    percentage
            );
        }

        model.addAttribute(
                "attendancePercentage",
                attendancePercentage
        );

        return "students";
    }

    @GetMapping("/register")
    public String registerPage(
            Model model,
            HttpSession session) {

        String role =
                (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/student-profile";
        }

        model.addAttribute(
                "student",
                new Student()
        );

        return "student-register";
    }

    @PostMapping("/saveStudent")
    public String saveStudent(
            @ModelAttribute Student student,
            HttpSession session) {

        String role =
                (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/student-profile";
        }

        studentService.saveStudent(student);

        return "redirect:/students";
    }

    @GetMapping("/editStudent/{id}")
    public String editStudent(
            @PathVariable Long id,
            Model model,
            HttpSession session) {

        String role =
                (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/students";
        }

        Student student =
                studentService.getStudentById(id);

        model.addAttribute(
                "student",
                student
        );

        return "student-register";
    }

    @GetMapping("/deleteStudent/{id}")
    public String deleteStudent(
            @PathVariable Long id,
            HttpSession session) {

        String role =
                (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/students";
        }

        studentService.deleteStudent(id);

        return "redirect:/students";
    }

    @GetMapping("/student-profile")
    public String studentProfile(
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (!"STUDENT".equals(role)) {
            return "redirect:/dashboard";
        }

        String email = (String) session.getAttribute("userEmail");

        if (email == null || email.trim().isEmpty()) {
            return "redirect:/login";
        }

        Student student = studentService.findByEmail(email);

        if (student == null) {
            model.addAttribute("studentNotFound", true);
            return "student-profile";
        }

        model.addAttribute("student", student);

        return "student-profile";
    }
}