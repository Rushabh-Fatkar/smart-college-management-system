package com.smartcollege.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartcollege.entity.Student;
import com.smartcollege.entity.User;
import com.smartcollege.repository.StudentRepository;
import com.smartcollege.repository.UserRepository;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;


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

        // First find the student
        Student student = studentRepository.findById(id).orElse(null);

        if (student != null) {

            // Find the related User account using student's email
            User user = userRepository.findByEmail(student.getEmail());

            // Delete User account if found
            if (user != null) {
                userRepository.delete(user);
            }

            // Delete Student record
            studentRepository.deleteById(id);
        }
    }


    public long getTotalStudents() {
        return studentRepository.count();
    }


    public Student findByName(String name) {
        return studentRepository.findByName(name);
    }


    public Student findByEmail(String email) {
        return studentRepository.findByEmail(email);
    }
}