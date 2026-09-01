package com.campusqueue.service;

import com.campusqueue.dto.request.CreateUserRequest;
import com.campusqueue.dto.response.UserResponse;
import com.campusqueue.entity.User;
import com.campusqueue.exception.ConflictException;
import com.campusqueue.exception.ResourceNotFoundException;
import com.campusqueue.repository.UserRepository;
import com.campusqueue.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new ConflictException("A user with email '" + request.getEmail() + "' already exists");
        }

        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : "student123";

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = new User(
                request.getName().trim(),
                request.getEmail().trim().toLowerCase(),
                request.getRole(),
                passwordHash
        );

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        // Enforce that students can only view their own user profile
        SecurityUtils.enforceUserOwnership(id, "view profile");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public User findUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
