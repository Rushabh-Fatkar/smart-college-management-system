package com.smartcollege.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.smartcollege.entity.Faculty;
import com.smartcollege.entity.User;
import com.smartcollege.service.FacultyService;
import com.smartcollege.service.UserService;

@Controller
public class FacultyController {

    @Autowired
    private FacultyService facultyService;

    @Autowired
    private UserService userService;

    @GetMapping("/faculty")
    public String viewFaculty(
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (role == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "facultyList",
                facultyService.getAllFaculty()
        );

        return "faculty";
    }

    @GetMapping("/faculty-register")
    public String facultyRegister(
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/student-profile";
        }

        model.addAttribute(
                "faculty",
                new Faculty()
        );

        model.addAttribute(
                "facultyUsername",
                ""
        );

        return "faculty-register";
    }

    @PostMapping("/saveFaculty")
    public String saveFaculty(
            @ModelAttribute Faculty faculty,
            @RequestParam String username,
            @RequestParam(required = false, defaultValue = "") String password,
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/student-profile";
        }

        /*
         * EDIT FACULTY
         */
        if (faculty.getId() != null) {

            User existingUser =
                    userService.findByUsername(username);

            if (existingUser == null) {

                model.addAttribute(
                        "error",
                        "Faculty login account not found."
                );

                model.addAttribute(
                        "faculty",
                        faculty
                );

                model.addAttribute(
                        "facultyUsername",
                        username
                );

                return "faculty-register";
            }

            facultyService.saveFaculty(faculty);

            existingUser.setEmail(
                    faculty.getEmail()
            );

            existingUser.setRole("FACULTY");

            if (password != null
                    && !password.trim().isEmpty()) {

                existingUser.setPassword(password);
            }

            userService.register(existingUser);

            return "redirect:/faculty";
        }

        /*
         * ADD NEW FACULTY
         */

        User existingUser =
                userService.findByUsername(username);

        if (existingUser != null) {

            model.addAttribute(
                    "error",
                    "Username already exists. Please choose another username."
            );

            model.addAttribute(
                    "faculty",
                    faculty
            );

            model.addAttribute(
                    "facultyUsername",
                    username
            );

            return "faculty-register";
        }

        facultyService.saveFaculty(faculty);

        User user = new User();

        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(faculty.getEmail());
        user.setRole("FACULTY");

        userService.register(user);

        return "redirect:/faculty";
    }

    @GetMapping("/editFaculty/{id}")
    public String editFaculty(
            @PathVariable Long id,
            Model model,
            HttpSession session) {

        String role =
                (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/faculty";
        }

        Faculty faculty =
                facultyService.getFacultyById(id);

        model.addAttribute(
                "faculty",
                faculty
        );

        /*
         * Faculty email वापरून त्याचा login User शोधतो
         */
        User user =
                userService.findByEmail(
                        faculty.getEmail()
                );

        if (user != null) {

            model.addAttribute(
                    "facultyUsername",
                    user.getUsername()
            );

        } else {

            model.addAttribute(
                    "facultyUsername",
                    ""
            );
        }

        return "faculty-register";
    }

    @GetMapping("/deleteFaculty/{id}")
    public String deleteFaculty(
            @PathVariable Long id,
            HttpSession session) {

        String role =
                (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/faculty";
        }

        facultyService.deleteFaculty(id);

        return "redirect:/faculty";
    }
}