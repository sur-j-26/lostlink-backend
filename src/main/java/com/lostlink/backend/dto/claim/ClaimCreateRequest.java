package com.lostlink.backend.dto.claim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimCreateRequest {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotBlank(message = "Verification details / identifying marks are required")
    private String verificationNotes;
}
