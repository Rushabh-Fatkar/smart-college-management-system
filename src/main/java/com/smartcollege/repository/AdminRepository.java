package com.smartcollege.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcollege.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Admin findByUsernameAndPassword(String username, String password);

}