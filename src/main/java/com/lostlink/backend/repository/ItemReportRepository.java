package com.lostlink.backend.repository;

import com.lostlink.backend.entity.ItemReport;
import com.lostlink.backend.entity.ItemStatus;
import com.lostlink.backend.entity.ReportType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemReportRepository extends JpaRepository<ItemReport, Long> {

    List<ItemReport> findByType(ReportType type, Sort sort);

    @Query("SELECT r FROM ItemReport r WHERE " +
           "(:type IS NULL OR r.type = :type) AND " +
           "(:categoryName IS NULL OR r.category.name = :categoryName) AND " +
           "(:query IS NULL OR (" +
           " LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(r.location) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<ItemReport> searchItems(
            @Param("type") ReportType type,
            @Param("categoryName") String categoryName,
            @Param("query") String query,
            Sort sort
    );

    @Query("SELECT r FROM ItemReport r WHERE " +
           "r.title = :title AND r.type = :type AND " +
           "r.location = :location AND r.description = :description AND " +
           "r.createdAt >= :since")
    List<ItemReport> findRecentDuplicates(
            @Param("title") String title,
            @Param("type") ReportType type,
            @Param("location") String location,
            @Param("description") String description,
            @Param("since") LocalDateTime since
    );

    List<ItemReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<ItemReport> findByTitleContainingIgnoreCase(String keyword);

    long countByType(ReportType type);

    long countByStatus(ItemStatus status);
}
