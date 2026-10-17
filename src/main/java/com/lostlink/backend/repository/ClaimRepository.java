package com.lostlink.backend.repository;

import com.lostlink.backend.entity.Claim;
import com.lostlink.backend.entity.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByItemId(Long itemId);
    List<Claim> findByClaimantIdOrderByCreatedAtDesc(Long claimantId);
    List<Claim> findByOrderByCreatedAtDesc();
    boolean existsByItemIdAndClaimantId(Long itemId, Long claimantId);
    long countByStatus(ClaimStatus status);
}
