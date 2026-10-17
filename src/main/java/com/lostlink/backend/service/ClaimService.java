package com.lostlink.backend.service;

import com.lostlink.backend.dto.claim.ClaimCreateRequest;
import com.lostlink.backend.dto.claim.ClaimDto;
import com.lostlink.backend.dto.claim.ClaimReviewRequest;
import com.lostlink.backend.entity.Claim;
import com.lostlink.backend.entity.ClaimStatus;
import com.lostlink.backend.entity.ItemReport;
import com.lostlink.backend.entity.ItemStatus;
import com.lostlink.backend.entity.User;
import com.lostlink.backend.exception.BadRequestException;
import com.lostlink.backend.exception.ResourceNotFoundException;
import com.lostlink.backend.exception.UnauthorizedException;
import com.lostlink.backend.repository.ClaimRepository;
import com.lostlink.backend.repository.ItemReportRepository;
import com.lostlink.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ItemReportRepository itemReportRepository;
    private final UserRepository userRepository;

    public ClaimService(
            ClaimRepository claimRepository,
            ItemReportRepository itemReportRepository,
            UserRepository userRepository
    ) {
        this.claimRepository = claimRepository;
        this.itemReportRepository = itemReportRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ClaimDto submitClaim(ClaimCreateRequest request, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Authentication required to submit an ownership claim");
        }

        User claimant = userRepository.findByEmail(userEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        ItemReport item = itemReportRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + request.getItemId()));

        if (item.getReporter() != null && item.getReporter().getId().equals(claimant.getId())) {
            throw new BadRequestException("You cannot submit an ownership claim on an item you reported yourself");
        }

        if (item.getStatus() == ItemStatus.CLAIMED || item.getStatus() == ItemStatus.RESOLVED) {
            throw new BadRequestException("This item has already been claimed or resolved");
        }

        if (claimRepository.existsByItemIdAndClaimantId(item.getId(), claimant.getId())) {
            throw new BadRequestException("You have already submitted an active claim for this item");
        }

        Claim claim = Claim.builder()
                .item(item)
                .claimant(claimant)
                .verificationNotes(request.getVerificationNotes().trim())
                .status(ClaimStatus.PENDING)
                .build();

        Claim saved = claimRepository.save(claim);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ClaimDto> getUserClaims(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }

        User claimant = userRepository.findByEmail(userEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return claimRepository.findByClaimantIdOrderByCreatedAtDesc(claimant.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClaimDto> getItemClaims(
            Long itemId,
            String userEmail,
            Collection<? extends GrantedAuthority> authorities
    ) {
        ItemReport item = itemReportRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        boolean isAdminOrStaff = authorities != null && authorities.stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF")
        );

        boolean isReporter = item.getReporter() != null &&
                userEmail != null &&
                item.getReporter().getEmail().equalsIgnoreCase(userEmail.trim());

        if (!isAdminOrStaff && !isReporter) {
            throw new AccessDeniedException("You are not authorized to inspect claims for this item report");
        }

        return claimRepository.findByItemId(itemId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClaimDto> getAllClaims(
            String userEmail,
            Collection<? extends GrantedAuthority> authorities
    ) {
        boolean isAdminOrStaff = authorities != null && authorities.stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF")
        );

        if (!isAdminOrStaff) {
            throw new AccessDeniedException("Administrative privileges required to inspect all campus claims");
        }

        return claimRepository.findByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClaimDto reviewClaim(
            Long claimId,
            ClaimReviewRequest request,
            String userEmail,
            Collection<? extends GrantedAuthority> authorities
    ) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Authentication required to review ownership claim");
        }

        User reviewer = userRepository.findByEmail(userEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer account not found"));

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        ItemReport item = claim.getItem();

        boolean isAdminOrStaff = authorities != null && authorities.stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF")
        );

        boolean isReporter = item != null &&
                item.getReporter() != null &&
                item.getReporter().getId().equals(reviewer.getId());

        if (!isAdminOrStaff && !isReporter) {
            throw new AccessDeniedException("Only campus administrators, staff, or the item reporter can review this claim");
        }

        if (claim.getClaimant() != null && claim.getClaimant().getId().equals(reviewer.getId())) {
            throw new BadRequestException("Claimant cannot review or approve their own ownership claim");
        }

        ClaimStatus newStatus = request.getStatus();
        if (newStatus != ClaimStatus.APPROVED && newStatus != ClaimStatus.REJECTED) {
            throw new BadRequestException("Claim review status must be either APPROVED or REJECTED");
        }

        claim.setStatus(newStatus);
        claim.setReviewedBy(reviewer);
        claim.setResolvedAt(LocalDateTime.now());
        if (request.getFeedback() != null && !request.getFeedback().isBlank()) {
            claim.setAdminFeedback(request.getFeedback().trim());
        }

        if (newStatus == ClaimStatus.APPROVED && item != null) {
            item.setStatus(ItemStatus.CLAIMED);
            itemReportRepository.save(item);

            // Automatically reject other pending claims for this item
            List<Claim> siblingClaims = claimRepository.findByItemId(item.getId());
            for (Claim sibling : siblingClaims) {
                if (!sibling.getId().equals(claim.getId()) && sibling.getStatus() == ClaimStatus.PENDING) {
                    sibling.setStatus(ClaimStatus.REJECTED);
                    sibling.setReviewedBy(reviewer);
                    sibling.setResolvedAt(LocalDateTime.now());
                    sibling.setAdminFeedback("Another ownership claim was approved for this item");
                    claimRepository.save(sibling);
                }
            }
        }

        Claim saved = claimRepository.save(claim);
        return mapToDto(saved);
    }

    public ClaimDto mapToDto(Claim claim) {
        ItemReport item = claim.getItem();
        User claimant = claim.getClaimant();
        User reviewer = claim.getReviewedBy();

        return ClaimDto.builder()
                .id(claim.getId())
                .itemId(item != null ? item.getId() : null)
                .itemTitle(item != null ? item.getTitle() : null)
                .itemType(item != null && item.getType() != null ? item.getType().name().toLowerCase() : null)
                .itemLocation(item != null ? item.getLocation() : null)
                .claimantId(claimant != null ? claimant.getId() : null)
                .claimantName(claimant != null ? claimant.getFullName() : null)
                .claimantEmail(claimant != null ? claimant.getEmail() : null)
                .verificationNotes(claim.getVerificationNotes())
                .status(claim.getStatus())
                .adminFeedback(claim.getAdminFeedback())
                .reviewedByName(reviewer != null ? reviewer.getFullName() : null)
                .createdAt(claim.getCreatedAt())
                .resolvedAt(claim.getResolvedAt())
                .build();
    }
}
