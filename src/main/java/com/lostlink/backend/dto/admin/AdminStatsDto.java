package com.lostlink.backend.dto.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDto {
    private long totalReports;
    private long lostReports;
    private long foundReports;
    private long totalClaims;
    private long pendingClaims;
    private long approvedClaims;
    private long resolvedItems;
    private long totalUsers;
}
