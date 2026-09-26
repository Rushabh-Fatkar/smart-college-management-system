package com.smartcollege.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartcollege.entity.Student;
import com.smartcollege.service.QrAttendanceService;
import com.smartcollege.service.StudentService;

@Controller
public class StudentAttendanceQrController {

    @Autowired
    private QrAttendanceService qrAttendanceService;

    @Autowired
    private StudentService studentService;

    /**
     * Student QR Scanner Page:
     * - Requests HTML5 Camera access.
     * - Provides manual session token input fallback.
     */
    @GetMapping("/student/scan-qr")
    public String showScannerPage(@RequestParam(required = false) String token, Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");

        // Role restriction: Only logged-in students can scan/submit a QR attendance request
        if (!"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("userEmail");
        Student student = studentService.findByEmail(email);
        model.addAttribute("student", student);
        model.addAttribute("prefillToken", token);

        return "student-qr-scan";
    }

    /**
     * AJAX Endpoint for Camera QR scanner:
     * Submits scanned token directly without GPS or location dependency.
     */
    @PostMapping("/student/api/submit-qr-attendance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitQrAttendanceAjax(
            @RequestParam String sessionToken,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        String role = (String) session.getAttribute("role");
        if (!"STUDENT".equals(role)) {
            response.put("success", false);
            response.put("message", "Access denied. Only logged-in students can mark QR attendance.");
            return ResponseEntity.status(403).body(response);
        }

        String studentEmail = (String) session.getAttribute("userEmail");
        if (studentEmail == null || studentEmail.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "User session expired. Please log in again.");
            return ResponseEntity.status(401).body(response);
        }

        QrAttendanceService.AttendanceResult result = qrAttendanceService.submitStudentAttendance(
                sessionToken,
                studentEmail
        );

        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());

        return ResponseEntity.ok(response);
    }

    /**
     * Form fallback submission for non-AJAX submissions.
     */
    @PostMapping("/student/submit-qr-attendance")
    public String submitQrAttendanceForm(
            @RequestParam String sessionToken,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String role = (String) session.getAttribute("role");
        if (!"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        String studentEmail = (String) session.getAttribute("userEmail");
        if (studentEmail == null || studentEmail.trim().isEmpty()) {
            return "redirect:/login";
        }

        QrAttendanceService.AttendanceResult result = qrAttendanceService.submitStudentAttendance(
                sessionToken,
                studentEmail
        );

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
        }

        return "redirect:/student/scan-qr";
    }
}
