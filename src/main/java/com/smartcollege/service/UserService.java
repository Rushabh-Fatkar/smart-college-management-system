package com.smartcollege.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartcollege.entity.User;
import com.smartcollege.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User login(String username, String password) {

        return userRepository.findByUsernameAndPassword(
                username,
                password
        );
    }

    public User register(User user) {

        return userRepository.save(user);
    }

    public User findByUsername(String username) {

        return userRepository.findByUsername(username);
    }

    public User findByEmail(String email) {

        return userRepository.findByEmail(email);
    }
}