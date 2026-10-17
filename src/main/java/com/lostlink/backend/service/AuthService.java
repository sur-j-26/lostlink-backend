package com.lostlink.backend.service;

import com.lostlink.backend.dto.auth.AuthResponse;
import com.lostlink.backend.dto.auth.LoginRequest;
import com.lostlink.backend.dto.auth.RegisterRequest;
import com.lostlink.backend.dto.auth.UserDto;
import com.lostlink.backend.entity.Role;
import com.lostlink.backend.entity.User;
import com.lostlink.backend.exception.BadRequestException;
import com.lostlink.backend.exception.ResourceNotFoundException;
import com.lostlink.backend.repository.UserRepository;
import com.lostlink.backend.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public static final String ANURAG_EMAIL_DOMAIN = "anurag.edu.in";
    private static final java.util.regex.Pattern ANURAG_EMAIL_PATTERN = java.util.regex.Pattern.compile(
            "^[A-Za-z0-9._%+-]+@anurag\\.edu\\.in$",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    public String validateAndNormalizeAnuragEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("University email address is required");
        }
        String trimmed = email.trim();
        if (!ANURAG_EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException("Please use your official Anurag University email address ending in @anurag.edu.in");
        }
        return trimmed.toLowerCase();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = validateAndNormalizeAnuragEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("An account with email " + normalizedEmail + " already exists");
        }

        // Secure Role Mapping: Never trust client input for privilege elevation
        Role userRole = determineSafeRole(request.getRole());

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .campusId(request.getStudentId() != null ? request.getStudentId().trim() : null)
                .role(userRole)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToUserDto(savedUser))
                .message("Account registered successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Performs password authentication via Spring Security and BCrypt
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalizedEmail));

        String token = tokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .message("Signed in successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToUserDto(user);
    }

    /**
     * Development-only method to safely reset the password of the test account.
     * Encodes the password using BCrypt via the configured PasswordEncoder.
     */
    @Transactional
    public void resetTestAccountPassword(String newPassword) {
        resetTestAccountPassword("test.student@campus.edu", newPassword);
    }

    @Transactional
    public void resetTestAccountPassword(String email, String newPassword) {
        String testEmail = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : "test.student@campus.edu";
        User user = userRepository.findByEmail(testEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Test account not found with email: " + testEmail));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Enforce strict role validation:
     * Never allow self-registration as ADMIN.
     */
    private Role determineSafeRole(String requestedRole) {
        if (requestedRole == null) {
            return Role.STUDENT;
        }

        String cleaned = requestedRole.trim().toUpperCase();
        switch (cleaned) {
            case "FACULTY":
                return Role.FACULTY;
            case "STAFF":
            case "SECURITY":
                return Role.STAFF;
            case "ADMIN":
                // Strictly rejected: Administrator accounts cannot be self-selected via public registration
                throw new BadRequestException("Administrator accounts cannot be created via public registration");
            case "STUDENT":
            default:
                return Role.STUDENT;
        }
    }

    public UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .campusId(user.getCampusId())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
