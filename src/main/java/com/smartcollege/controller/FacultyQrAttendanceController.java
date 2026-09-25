package com.smartcollege.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.smartcollege.entity.Attendance;
import com.smartcollege.entity.AttendanceSession;
import com.smartcollege.entity.Course;
import com.smartcollege.entity.Student;
import com.smartcollege.service.CourseService;
import com.smartcollege.service.QrAttendanceService;

@Controller
public class FacultyQrAttendanceController {

    @Autowired
    private QrAttendanceService qrAttendanceService;

    @Autowired
    private CourseService courseService;

    @Value("${attendance.qr.allowed-radius-meters:100.0}")
    private double defaultRadius;

    /**
     * Step 1: Faculty selects course and captures classroom location.
     */
    @GetMapping("/faculty/qr-attendance")
    public String showQrGenerationPage(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");

        // Role restriction: Only logged-in Faculty users can generate attendance QR codes
        if (!"FACULTY".equals(role)) {
            return "redirect:/login";
        }

        List<Course> courses = courseService.getAllCourses();
        model.addAttribute("courses", courses != null ? courses : Collections.emptyList());
        model.addAttribute("defaultRadius", defaultRadius);

        String facultyEmail = (String) session.getAttribute("userEmail");
        if (facultyEmail != null) {
            List<AttendanceSession> previousSessions = qrAttendanceService.getSessionsByFaculty(facultyEmail);
            model.addAttribute("previousSessions", previousSessions != null ? previousSessions : Collections.emptyList());
        } else {
            model.addAttribute("previousSessions", Collections.emptyList());
        }

        return "faculty-qr-session";
    }

    /**
     * Step 2: Faculty clicks "Generate QR" -> creates new 2-minute unique session.
     */
    @PostMapping("/faculty/generate-qr")
    public String generateQrSession(@RequestParam String courseName,
                                    @RequestParam(required = false) Double latitude,
                                    @RequestParam(required = false) Double longitude,
                                    @RequestParam(required = false) Double radiusMeters,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"FACULTY".equals(role)) {
            return "redirect:/login";
        }

        if (courseName == null || courseName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a valid course.");
            return "redirect:/faculty/qr-attendance";
        }

        String facultyUsername = (String) session.getAttribute("username");
        String facultyEmail = (String) session.getAttribute("userEmail");

        // If faculty device coordinates were not provided or denied, use 0.0 default or prompt
        Double facultyLat = (latitude != null) ? latitude : 0.0;
        Double facultyLng = (longitude != null) ? longitude : 0.0;
        Double radius = (radiusMeters != null && radiusMeters > 0) ? radiusMeters : defaultRadius;

        AttendanceSession newSession = qrAttendanceService.createSession(
                courseName.trim(),
                facultyUsername,
                facultyEmail,
                facultyLat,
                facultyLng,
                radius
        );

        return "redirect:/faculty/session/" + newSession.getSessionToken();
    }

    /**
     * Step 3: Faculty live session dashboard showing QR code, timer, and live attendees.
     */
    @GetMapping("/faculty/session/{sessionToken}")
    public String viewSession(@PathVariable String sessionToken, Model model, HttpSession session, HttpServletRequest request) {
        String role = (String) session.getAttribute("role");
        if (!"FACULTY".equals(role)) {
            return "redirect:/login";
        }

        Optional<AttendanceSession> sessionOpt = qrAttendanceService.getSessionByToken(sessionToken);
        if (sessionOpt.isEmpty()) {
            return "redirect:/faculty/qr-attendance";
        }

        AttendanceSession attendanceSession = sessionOpt.get();

        // Dynamically compute base URL without hardcoding localhost (compatible with Render / reverse proxy)
        String baseUrl;
        try {
            baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(null)
                    .build()
                    .toUriString();
        } catch (Exception e) {
            baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        }

        String qrUrl = baseUrl + "/student/scan-qr?token=" + attendanceSession.getSessionToken();
        String qrImage = qrAttendanceService.generateQrCodeBase64(qrUrl);

        long secondsRemaining = 0;
        if (!attendanceSession.isExpired() && attendanceSession.isActive()) {
            secondsRemaining = Duration.between(LocalDateTime.now(), attendanceSession.getExpiresAt()).getSeconds();
            if (secondsRemaining < 0) {
                secondsRemaining = 0;
            }
        }

        List<Attendance> attendees = qrAttendanceService.getAttendeesForSession(sessionToken);

        model.addAttribute("attendanceSession", attendanceSession);
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("qrImage", qrImage);
        model.addAttribute("secondsRemaining", secondsRemaining);
        model.addAttribute("isExpired", attendanceSession.isExpired() || !attendanceSession.isActive());
        model.addAttribute("attendees", attendees != null ? attendees : Collections.emptyList());

        return "faculty-qr-session";
    }

    /**
     * Generate New QR action: Closes/deactivates the active session and navigates to the generation form.
     */
    @GetMapping("/faculty/generate-new-qr/{sessionToken}")
    public String generateNewQr(@PathVariable String sessionToken, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"FACULTY".equals(role)) {
            return "redirect:/login";
        }

        qrAttendanceService.deactivateSession(sessionToken);
        return "redirect:/faculty/qr-attendance";
    }

    /**
     * Live Polling API for the faculty page to update attendees and countdown in real time.
     */
    @GetMapping("/faculty/api/session-status/{sessionToken}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLiveSessionStatus(@PathVariable String sessionToken, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"FACULTY".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        Optional<AttendanceSession> sessionOpt = qrAttendanceService.getSessionByToken(sessionToken);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AttendanceSession attendanceSession = sessionOpt.get();
        long secondsRemaining = 0;
        if (!attendanceSession.isExpired() && attendanceSession.isActive()) {
            secondsRemaining = Duration.between(LocalDateTime.now(), attendanceSession.getExpiresAt()).getSeconds();
            if (secondsRemaining < 0) {
                secondsRemaining = 0;
            }
        }

        List<Attendance> attendees = qrAttendanceService.getAttendeesForSession(sessionToken);

        Map<String, Object> data = new HashMap<>();
        data.put("expired", attendanceSession.isExpired() || !attendanceSession.isActive());
        data.put("secondsRemaining", secondsRemaining);
        data.put("totalPresent", attendees != null ? attendees.size() : 0);
        data.put("attendees", attendees != null ? attendees : Collections.emptyList());

        return ResponseEntity.ok(data);
    }

    /**
     * Faculty Manual Attendance: Page to mark missed students for this session.
     */
    @GetMapping("/faculty/manual-attendance/{sessionToken}")
    public String showManualAttendancePage(@PathVariable String sessionToken, Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"FACULTY".equals(role)) {
            return "redirect:/login";
        }

        Optional<AttendanceSession> sessionOpt = qrAttendanceService.getSessionByToken(sessionToken);
        if (sessionOpt.isEmpty()) {
            return "redirect:/faculty/qr-attendance";
        }

        AttendanceSession attendanceSession = sessionOpt.get();
        List<Student> unmarkedStudents = qrAttendanceService.getUnmarkedStudentsForSession(sessionToken);
        List<Attendance> attendees = qrAttendanceService.getAttendeesForSession(sessionToken);

        model.addAttribute("attendanceSession", attendanceSession);
        model.addAttribute("unmarkedStudents", unmarkedStudents != null ? unmarkedStudents : Collections.emptyList());
        model.addAttribute("attendees", attendees != null ? attendees : Collections.emptyList());

        return "faculty-manual-attendance";
    }

    /**
     * Faculty Manual Attendance: Mark a student Present.
     */
    @PostMapping("/faculty/mark-manual")
    public String markManual(@RequestParam String sessionToken,
                             @RequestParam Long studentId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"FACULTY".equals(role)) {
            return "redirect:/login";
        }

        String facultyEmail = (String) session.getAttribute("userEmail");
        QrAttendanceService.AttendanceResult result = qrAttendanceService.markManualAttendance(sessionToken, studentId, facultyEmail);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
        }

        return "redirect:/faculty/manual-attendance/" + sessionToken;
    }
}
