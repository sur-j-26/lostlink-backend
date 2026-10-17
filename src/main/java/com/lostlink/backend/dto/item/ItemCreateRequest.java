package com.lostlink.backend.dto.item;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Type is required (lost or found)")
    private String type; // "lost" | "found"

    private String category;

    @NotBlank(message = "Location is required")
    private String location;

    private String date;

    @NotBlank(message = "Description is required")
    private String description;

    private String contactInfo;
}
