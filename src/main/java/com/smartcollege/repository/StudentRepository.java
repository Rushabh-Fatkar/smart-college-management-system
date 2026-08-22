package com.smartcollege.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smartcollege.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Student findByName(String name);

}