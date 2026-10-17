package com.lostlink.backend.controller;

import com.lostlink.backend.dto.admin.AdminReportStatusUpdateRequest;
import com.lostlink.backend.dto.admin.AdminStatsDto;
import com.lostlink.backend.dto.auth.UserDto;
import com.lostlink.backend.dto.claim.ClaimDto;
import com.lostlink.backend.dto.common.ApiResponse;
import com.lostlink.backend.dto.item.ItemReportDto;
import com.lostlink.backend.entity.ClaimStatus;
import com.lostlink.backend.entity.ItemReport;
import com.lostlink.backend.entity.ItemStatus;
import com.lostlink.backend.entity.ReportType;
import com.lostlink.backend.exception.ResourceNotFoundException;
import com.lostlink.backend.repository.ClaimRepository;
import com.lostlink.backend.repository.ItemReportRepository;
import com.lostlink.backend.repository.UserRepository;
import com.lostlink.backend.service.AuthService;
import com.lostlink.backend.service.ClaimService;
import com.lostlink.backend.service.ItemReportService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminController {

    private final ItemReportRepository itemReportRepository;
    private final ItemReportService itemReportService;
    private final ClaimRepository claimRepository;
    private final ClaimService claimService;
    private final UserRepository userRepository;
    private final AuthService authService;

    public AdminController(
            ItemReportRepository itemReportRepository,
            ItemReportService itemReportService,
            ClaimRepository claimRepository,
            ClaimService claimService,
            UserRepository userRepository,
            AuthService authService
    ) {
        this.itemReportRepository = itemReportRepository;
        this.itemReportService = itemReportService;
        this.claimRepository = claimRepository;
        this.claimService = claimService;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDto>> getStats() {
        AdminStatsDto stats = AdminStatsDto.builder()
                .totalReports(itemReportRepository.count())
                .lostReports(itemReportRepository.countByType(ReportType.LOST))
                .foundReports(itemReportRepository.countByType(ReportType.FOUND))
                .totalClaims(claimRepository.count())
                .pendingClaims(claimRepository.countByStatus(ClaimStatus.PENDING))
                .approvedClaims(claimRepository.countByStatus(ClaimStatus.APPROVED))
                .resolvedItems(itemReportRepository.countByStatus(ItemStatus.CLAIMED) + itemReportRepository.countByStatus(ItemStatus.RESOLVED))
                .totalUsers(userRepository.count())
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers() {
        List<UserDto> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(authService::mapToUserDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<ItemReportDto>>> getReports() {
        List<ItemReportDto> reports = itemReportService.getAllReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PatchMapping("/reports/{id}/status")
    public ResponseEntity<ApiResponse<ItemReportDto>> updateReportStatus(
            @PathVariable Long id,
            @RequestBody AdminReportStatusUpdateRequest request
    ) {
        ItemReportDto updated = itemReportService.updateReportStatusByAdmin(id, request.getStatus(), request.getVerifiedBySecurity());
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", updated));
    }

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<List<ClaimDto>>> getClaims(Authentication authentication) {
        List<ClaimDto> claims = claimService.getAllClaims(authentication.getName(), authentication.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success(claims));
    }
}
