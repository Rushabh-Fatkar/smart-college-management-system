package com.smartcollege.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartcollege.entity.Student;
import com.smartcollege.repository.StudentRepository;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    public Student saveStudent(Student student) {
        return studentRepository.save(student);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }
    public Student getStudentById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }
    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }
    public long getTotalStudents() {
        return studentRepository.count();
    }
    public Student findByName(String name) {
        return studentRepository.findByName(name);
    }
}
