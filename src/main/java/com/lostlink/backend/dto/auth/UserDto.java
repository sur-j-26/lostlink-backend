package com.lostlink.backend.dto.auth;

import com.lostlink.backend.entity.Role;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String fullName;
    private String campusId;
    private Role role;
    private LocalDateTime createdAt;
}
