package com.lostlink.backend.dto.item;

import com.lostlink.backend.entity.MatchStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemMatchDto {
    private Long id;
    private Long lostItemId;
    private String lostItemTitle;
    private Long foundItemId;
    private String foundItemTitle;
    private Double confidenceScore;
    private MatchStatus status;
    private String matchNotes;
    private LocalDateTime createdAt;
}
