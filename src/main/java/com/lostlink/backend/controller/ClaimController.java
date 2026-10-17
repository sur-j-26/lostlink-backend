package com.lostlink.backend.controller;

import com.lostlink.backend.dto.claim.ClaimCreateRequest;
import com.lostlink.backend.dto.claim.ClaimDto;
import com.lostlink.backend.dto.claim.ClaimReviewRequest;
import com.lostlink.backend.dto.common.ApiResponse;
import com.lostlink.backend.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClaimDto>> submitClaim(
            @Valid @RequestBody ClaimCreateRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to submit an ownership claim"));
        }
        ClaimDto created = claimService.submitClaim(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ownership claim submitted for review", created));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ClaimDto>>> getMyClaims(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        List<ClaimDto> claims = claimService.getUserClaims(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(claims));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<List<ClaimDto>>> getItemClaims(
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        List<ClaimDto> claims = claimService.getItemClaims(itemId, authentication.getName(), authentication.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success(claims));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ClaimDto>>> getAllClaims(Authentication authentication) {
        List<ClaimDto> claims = claimService.getAllClaims(authentication.getName(), authentication.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success(claims));
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<ApiResponse<ClaimDto>> reviewClaim(
            @PathVariable Long id,
            @Valid @RequestBody ClaimReviewRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to review ownership claim"));
        }
        ClaimDto reviewed = claimService.reviewClaim(id, request, authentication.getName(), authentication.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success("Claim status updated successfully", reviewed));
    }
}
