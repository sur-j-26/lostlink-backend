package com.lostlink.backend.dto.item;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemReportDto {
    private Long id;
    private String title;
    private String description;
    private String type; // "lost" | "found"
    private String category;
    private String location;
    private String date;
    private String formattedDate;
    private String status;
    private String contactInfo;
    private boolean securityVerified;
    private String aiConfidence;
    private Long reporterId;
    private String reporterEmail;
}
