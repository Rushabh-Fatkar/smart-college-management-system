package com.smartcollege.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.smartcollege.entity.Course;
import com.smartcollege.service.CourseService;

@Controller
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/courses")
    public String viewCourses(
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (role == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "courseList",
                courseService.getAllCourses()
        );

        return "course";
    }

    @GetMapping("/course-register")
    public String courseRegister(
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/courses";
        }

        model.addAttribute(
                "course",
                new Course()
        );

        return "course-register";
    }

    @PostMapping("/saveCourse")
    public String saveCourse(
            @ModelAttribute Course course,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/courses";
        }

        courseService.saveCourse(course);

        return "redirect:/courses";
    }

    @GetMapping("/editCourse/{id}")
    public String editCourse(
            @PathVariable Long id,
            Model model,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/courses";
        }

        model.addAttribute(
                "course",
                courseService.getCourseById(id)
        );

        return "course-register";
    }

    @GetMapping("/deleteCourse/{id}")
    public String deleteCourse(
            @PathVariable Long id,
            HttpSession session) {

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)
                && !"FACULTY".equals(role)) {

            return "redirect:/courses";
        }

        courseService.deleteCourse(id);

        return "redirect:/courses";
    }
}