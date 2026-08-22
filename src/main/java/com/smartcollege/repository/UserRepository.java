package com.smartcollege.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcollege.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsernameAndPassword(String username, String password);

    User findByUsername(String username);

    User findByEmail(String email);
}