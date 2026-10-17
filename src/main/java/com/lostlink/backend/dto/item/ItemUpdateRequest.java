package com.lostlink.backend.dto.item;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemUpdateRequest {
    private String title;
    private String type; // "lost" | "found"
    private String category;
    private String location;
    private String date;
    private String description;
    private String contactInfo;
    private String status; // "REPORTED", "HELD_AT_DESK", "CLAIMED", "RESOLVED", "CLOSED"
}
