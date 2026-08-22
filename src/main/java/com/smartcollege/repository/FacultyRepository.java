package com.smartcollege.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcollege.entity.Faculty;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

}