package com.lostlink.backend.dto.claim;

import com.lostlink.backend.entity.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimReviewRequest {

    @NotNull(message = "Review status is required (APPROVED or REJECTED)")
    private ClaimStatus status;

    private String feedback;
}
