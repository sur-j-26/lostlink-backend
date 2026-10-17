package com.lostlink.backend.repository;

import com.lostlink.backend.entity.ItemMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemMatchRepository extends JpaRepository<ItemMatch, Long> {
    List<ItemMatch> findByLostItemId(Long lostItemId);
    List<ItemMatch> findByFoundItemId(Long foundItemId);
    Optional<ItemMatch> findByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);
    List<ItemMatch> findByLostItemIdOrFoundItemId(Long lostItemId, Long foundItemId);
}
