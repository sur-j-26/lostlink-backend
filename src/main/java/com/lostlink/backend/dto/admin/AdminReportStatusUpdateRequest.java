package com.lostlink.backend.dto.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReportStatusUpdateRequest {
    private String status;
    private Boolean verifiedBySecurity;
}
