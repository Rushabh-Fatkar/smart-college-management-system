package com.smartcollege.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcollege.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

}