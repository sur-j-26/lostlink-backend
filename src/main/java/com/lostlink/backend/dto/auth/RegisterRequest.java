package com.lostlink.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Campus email is required")
    @Email(message = "Must be a valid email address")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@anurag\\.edu\\.in$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Please use your Anurag University email address ending in @anurag.edu.in"
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    private String studentId;

    private String role; // Client-supplied role hint; validated securely on backend
}
