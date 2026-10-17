package com.lostlink.backend.controller;

import com.lostlink.backend.dto.claim.ClaimCreateRequest;
import com.lostlink.backend.dto.claim.ClaimDto;
import com.lostlink.backend.dto.common.ApiResponse;
import com.lostlink.backend.dto.item.ItemCreateRequest;
import com.lostlink.backend.dto.item.ItemReportDto;
import com.lostlink.backend.dto.item.ItemUpdateRequest;
import com.lostlink.backend.service.ClaimService;
import com.lostlink.backend.service.ItemReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemReportController {

    private final ItemReportService itemReportService;
    private final ClaimService claimService;
    private final com.lostlink.backend.service.MatchingService matchingService;

    public ItemReportController(
            ItemReportService itemReportService,
            ClaimService claimService,
            com.lostlink.backend.service.MatchingService matchingService
    ) {
        this.itemReportService = itemReportService;
        this.claimService = claimService;
        this.matchingService = matchingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemReportDto>>> getItems(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "false") Boolean aiMatchesOnly
    ) {
        List<ItemReportDto> items = itemReportService.searchItems(type, category, query, sort, aiMatchesOnly);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemReportDto>> getItemById(@PathVariable Long id) {
        ItemReportDto item = itemReportService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<ApiResponse<List<com.lostlink.backend.dto.item.ItemMatchDto>>> getItemMatches(@PathVariable Long id) {
        List<com.lostlink.backend.dto.item.ItemMatchDto> matches = matchingService.getMatchesForItem(id);
        return ResponseEntity.ok(ApiResponse.success(matches));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ItemReportDto>> createItem(
            @Valid @RequestBody ItemCreateRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ItemReportDto created = itemReportService.createItem(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report created successfully", created));
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<ClaimDto>> submitClaim(
            @PathVariable Long id,
            @Valid @RequestBody ClaimCreateRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("You must be logged in to submit an ownership claim"));
        }
        request.setItemId(id);
        ClaimDto claimDto = claimService.submitClaim(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ownership claim submitted for review", claimDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemReportDto>> updateItem(
            @PathVariable Long id,
            @RequestBody ItemUpdateRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to update this report"));
        }
        ItemReportDto updated = itemReportService.updateItem(id, request, authentication.getName(), authentication.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to delete this report"));
        }
        itemReportService.deleteItem(id, authentication.getName(), authentication.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success("Report deleted successfully", null));
    }
}
