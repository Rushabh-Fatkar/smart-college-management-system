package com.smartcollege.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartcollege.entity.Admin;
import com.smartcollege.repository.AdminRepository;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    public Admin login(String username, String password) {

        return adminRepository.findByUsernameAndPassword(username, password);

    }
    public Admin register(Admin admin) {
        return adminRepository.save(admin);
    }
}