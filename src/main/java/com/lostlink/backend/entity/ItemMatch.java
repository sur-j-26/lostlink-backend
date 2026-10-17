package com.lostlink.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_matches", indexes = {
    @Index(name = "idx_match_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_item_id", nullable = false)
    private ItemReport lostItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_item_id", nullable = false)
    private ItemReport foundItem;

    @Column(nullable = false)
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MatchStatus status = MatchStatus.POTENTIAL;

    @Column(columnDefinition = "TEXT")
    private String matchNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = MatchStatus.POTENTIAL;
        }
    }
}
