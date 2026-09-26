package com.smartcollege.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartcollege.entity.Attendance;
import com.smartcollege.repository.AttendanceRepository;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    public Attendance saveAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    public Attendance getAttendanceById(Long id) {
        return attendanceRepository.findById(id).orElse(null);
    }

    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
    }

    public long getTotalAttendance() {
        return attendanceRepository.count();
    }
    public Attendance findByStudentNameAndDate(String studentName, String date) {
        return attendanceRepository.findByStudentNameAndDate(studentName, date);
    }

    public Attendance findByStudentNameAndCourseNameAndLectureNumberAndDate(String studentName, String courseName, Integer lectureNumber, String date) {
        return attendanceRepository.findByStudentNameAndCourseNameAndLectureNumberAndDate(studentName, courseName, lectureNumber, date);
    }

    public boolean isAlreadyMarkedForLecture(String studentName, String studentEmail, String courseName, Integer lectureNumber, String date) {
        if (studentEmail != null && !studentEmail.trim().isEmpty()) {
            if (attendanceRepository.existsByCourseNameAndLectureNumberAndDateAndStudentEmail(courseName, lectureNumber, date, studentEmail.trim())) {
                return true;
            }
        }
        if (studentName != null && !studentName.trim().isEmpty()) {
            return attendanceRepository.existsByCourseNameAndLectureNumberAndDateAndStudentName(courseName, lectureNumber, date, studentName.trim());
        }
        return false;
    }

    public List<Attendance> getAllAttendanceOrdered() {
        return attendanceRepository.findAllByOrderByDateDescIdDesc();
    }

    public List<Attendance> getAttendanceByStudentEmail(String studentEmail) {
        return attendanceRepository.findByStudentEmailOrderByDateDesc(studentEmail);
    }

    public List<Attendance> getAttendanceByStudentName(String studentName) {
        return attendanceRepository.findByStudentNameOrderByDateDesc(studentName);
    }

    public long getTotalAttendanceByStudent(String studentName) {
        return attendanceRepository.getTotalAttendanceByStudent(studentName);
    }

    public long getPresentAttendanceByStudent(String studentName) {
        return attendanceRepository.getPresentAttendanceByStudent(studentName);
    }
}