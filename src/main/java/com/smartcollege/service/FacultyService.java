package com.smartcollege.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartcollege.entity.Faculty;
import com.smartcollege.repository.FacultyRepository;

@Service
public class FacultyService {

    @Autowired
    private FacultyRepository facultyRepository;

    public Faculty saveFaculty(Faculty faculty) {
        return facultyRepository.save(faculty);
    }

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public long getTotalFaculty() {
        return facultyRepository.count();
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id).orElse(null);
    }

    public void deleteFaculty(Long id) {
        facultyRepository.deleteById(id);
    }

}