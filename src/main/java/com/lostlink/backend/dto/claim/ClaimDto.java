package com.lostlink.backend.dto.claim;

import com.lostlink.backend.entity.ClaimStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimDto {
    private Long id;
    private Long itemId;
    private String itemTitle;
    private String itemType;
    private String itemLocation;
    private Long claimantId;
    private String claimantName;
    private String claimantEmail;
    private String verificationNotes;
    private ClaimStatus status;
    private String adminFeedback;
    private String reviewedByName;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
