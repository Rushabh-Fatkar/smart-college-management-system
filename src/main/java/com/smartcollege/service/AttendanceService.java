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
    public long getTotalAttendanceByStudent(String studentName) {
        return attendanceRepository.getTotalAttendanceByStudent(studentName);
    }

    public long getPresentAttendanceByStudent(String studentName) {
        return attendanceRepository.getPresentAttendanceByStudent(studentName);
    }
}