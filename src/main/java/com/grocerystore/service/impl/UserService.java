package com.grocerystore.service.impl;

import com.grocerystore.dto.request.ChangePasswordRequest;
import com.grocerystore.dto.request.UpdateProfileRequest;
import com.grocerystore.dto.response.UserResponse;
import com.grocerystore.entity.User;
import com.grocerystore.exception.BadRequestException;
import com.grocerystore.exception.ResourceNotFoundException;
import com.grocerystore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getCurrentUser(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    public UserResponse updateProfile(@NonNull Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        return toResponse(userRepository.save(Objects.requireNonNull(user)));
    }

    public void changePassword(@NonNull Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new BadRequestException("Current password is incorrect");

        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new BadRequestException("New passwords do not match");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(Objects.requireNonNull(user));
    }

    public Page<UserResponse> getAllUsers(@NonNull Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public UserResponse toggleUserStatus(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(!user.getEnabled());
        return toResponse(userRepository.save(Objects.requireNonNull(user)));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
