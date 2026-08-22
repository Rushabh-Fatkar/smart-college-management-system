package com.smartcollege.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.smartcollege.entity.Admin;
import com.smartcollege.entity.User;
import com.smartcollege.service.AdminService;
import com.smartcollege.service.AttendanceService;
import com.smartcollege.service.CourseService;
import com.smartcollege.service.FacultyService;
import com.smartcollege.service.StudentService;
import com.smartcollege.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private FacultyService facultyService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("/login")
    public String loginPage(Model model) {

        model.addAttribute("admin", new Admin());

        return "login";
    }

    @PostMapping("/login")
    public String login(
            @ModelAttribute Admin admin,
            Model model,
            HttpSession session) {

        Admin validAdmin = adminService.login(
                admin.getUsername(),
                admin.getPassword()
        );

        if (validAdmin != null) {

            session.setAttribute("username", validAdmin.getUsername());
            session.setAttribute("role", "ADMIN");

            return "redirect:/dashboard";
        }

        User validUser = userService.login(
                admin.getUsername(),
                admin.getPassword()
        );

        if (validUser != null) {

            session.setAttribute("username", validUser.getUsername());
            session.setAttribute("role", validUser.getRole());
            session.setAttribute("userEmail", validUser.getEmail());

            if ("STUDENT".equals(validUser.getRole())) {

                return "redirect:/student-profile";
            }

            if ("FACULTY".equals(validUser.getRole())) {

                return "redirect:/faculty";
            }
        }

        model.addAttribute(
                "error",
                "Invalid Username or Password"
        );

        model.addAttribute("admin", new Admin());

        return "login";
    }

    @GetMapping("/admin-register")
    public String adminRegisterPage(Model model) {

        model.addAttribute("admin", new Admin());

        return "admin-register";
    }

    @PostMapping("/saveAdmin")
    public String saveAdmin(
            @ModelAttribute Admin admin,
            Model model) {

        adminService.register(admin);

        model.addAttribute(
                "success",
                "Registration successful. Please login."
        );

        model.addAttribute("admin", new Admin());

        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (role == null) {
            return "redirect:/login";
        }

        if ("STUDENT".equals(role)) {
            return "redirect:/student-profile";
        }

        model.addAttribute(
                "totalStudents",
                studentService.getTotalStudents()
        );

        model.addAttribute(
                "totalFaculty",
                facultyService.getTotalFaculty()
        );

        model.addAttribute(
                "totalCourses",
                courseService.getTotalCourses()
        );

        model.addAttribute(
                "totalAttendance",
                attendanceService.getTotalAttendance()
        );

        return "dashboard";
    }

    @GetMapping("/user-register")
    public String userRegisterPage(Model model) {

        model.addAttribute("user", new User());

        return "register";
    }

    @PostMapping("/registerUser")
    public String registerUser(
            @ModelAttribute User user,
            Model model) {

        User existingUser =
                userService.findByUsername(user.getUsername());

        if (existingUser != null) {

            model.addAttribute(
                    "error",
                    "Username already exists. Please choose another username."
            );

            model.addAttribute("user", user);

            return "register";
        }

        user.setRole("STUDENT");

        userService.register(user);

        model.addAttribute(
                "success",
                "Registration successful. Please login."
        );

        model.addAttribute("admin", new Admin());

        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }
}