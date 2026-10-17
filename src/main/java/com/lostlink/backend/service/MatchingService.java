package com.lostlink.backend.service;

import com.lostlink.backend.dto.item.ItemMatchDto;
import com.lostlink.backend.entity.ItemMatch;
import com.lostlink.backend.entity.ItemReport;
import com.lostlink.backend.entity.ItemStatus;
import com.lostlink.backend.entity.MatchStatus;
import com.lostlink.backend.entity.ReportType;
import com.lostlink.backend.repository.ItemMatchRepository;
import com.lostlink.backend.repository.ItemReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "in", "on", "at", "to", "for", "with", "of",
            "from", "by", "is", "it", "my", "this", "that", "has", "have", "left", "found",
            "lost", "near", "after", "room", "floor", "hall", "desk", "small", "large"
    );

    private final ItemMatchRepository itemMatchRepository;
    private final ItemReportRepository itemReportRepository;

    public MatchingService(ItemMatchRepository itemMatchRepository, ItemReportRepository itemReportRepository) {
        this.itemMatchRepository = itemMatchRepository;
        this.itemReportRepository = itemReportRepository;
    }

    /**
     * Rule-based explainable comparison between a LOST report and a FOUND report.
     */
    public MatchEvaluation evaluate(ItemReport lostItem, ItemReport foundItem) {
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        // 1. Category Matching (Weight: 35%)
        boolean sameCategory = lostItem.getCategory() != null &&
                foundItem.getCategory() != null &&
                lostItem.getCategory().getName().equalsIgnoreCase(foundItem.getCategory().getName());

        if (sameCategory) {
            score += 35.0;
            reasons.add("Matching category: " + lostItem.getCategory().getName());
        }

        // 2. Title Keyword Similarity (Weight: 30%)
        Set<String> lostTitleWords = extractKeywords(lostItem.getTitle());
        Set<String> foundTitleWords = extractKeywords(foundItem.getTitle());
        Set<String> commonTitleWords = new HashSet<>(lostTitleWords);
        commonTitleWords.retainAll(foundTitleWords);

        if (!commonTitleWords.isEmpty()) {
            double titleOverlapRatio = (double) commonTitleWords.size() /
                    Math.max(1, Math.min(lostTitleWords.size(), foundTitleWords.size()));
            double titleScore = Math.min(30.0, titleOverlapRatio * 30.0);
            score += titleScore;
            reasons.add("Shared title keywords: [" + String.join(", ", commonTitleWords) + "]");
        } else if (lostItem.getTitle() != null && foundItem.getTitle() != null) {
            String lt = lostItem.getTitle().toLowerCase();
            String ft = foundItem.getTitle().toLowerCase();
            if (lt.contains(ft) || ft.contains(lt)) {
                score += 20.0;
                reasons.add("Title phrase overlap");
            }
        }

        // 3. Description Keyword Similarity (Weight: 15%)
        Set<String> lostDescWords = extractKeywords(lostItem.getDescription());
        Set<String> foundDescWords = extractKeywords(foundItem.getDescription());
        Set<String> commonDescWords = new HashSet<>(lostDescWords);
        commonDescWords.retainAll(foundDescWords);

        if (!commonDescWords.isEmpty()) {
            double descOverlapRatio = (double) commonDescWords.size() /
                    Math.max(1, Math.max(lostDescWords.size(), foundDescWords.size()));
            double descScore = Math.min(15.0, descOverlapRatio * 30.0);
            score += descScore;
            reasons.add("Shared description details: [" + String.join(", ", commonDescWords) + "]");
        }

        // 4. Campus Location Proximity (Weight: 10%)
        Set<String> lostLocWords = extractKeywords(lostItem.getLocation());
        Set<String> foundLocWords = extractKeywords(foundItem.getLocation());
        Set<String> commonLocWords = new HashSet<>(lostLocWords);
        commonLocWords.retainAll(foundLocWords);

        if (!commonLocWords.isEmpty()) {
            score += 10.0;
            reasons.add("Nearby campus location: [" + String.join(", ", commonLocWords) + "]");
        }

        // 5. Date Proximity (Weight: 10%)
        if (lostItem.getItemDate() != null && foundItem.getItemDate() != null) {
            long daysApart = Math.abs(ChronoUnit.DAYS.between(lostItem.getItemDate(), foundItem.getItemDate()));
            if (daysApart <= 2) {
                score += 10.0;
                reasons.add("Reported within " + (daysApart == 0 ? "same day" : daysApart + " days"));
            } else if (daysApart <= 7) {
                score += 5.0;
                reasons.add("Reported within a week");
            } else if (daysApart <= 14) {
                score += 2.0;
            }
        }

        double finalScore = Math.min(100.0, Math.round(score * 10.0) / 10.0);
        String explanation = String.format("%.0f%% match — %s", finalScore, String.join("; ", reasons));
        return new MatchEvaluation(finalScore, explanation);
    }

    /**
     * Executes AI match processing for a newly created or updated report.
     */
    @Transactional
    public void processMatchesForReport(ItemReport report) {
        if (report == null || report.getType() == null) {
            return;
        }

        ReportType counterType = report.getType() == ReportType.LOST ? ReportType.FOUND : ReportType.LOST;
        List<ItemReport> candidates = itemReportRepository.findByType(counterType, Sort.by(Sort.Direction.DESC, "createdAt"));

        for (ItemReport candidate : candidates) {
            // Only evaluate active reports
            if (candidate.getStatus() == ItemStatus.CLAIMED || candidate.getStatus() == ItemStatus.RESOLVED) {
                continue;
            }

            ItemReport lost = report.getType() == ReportType.LOST ? report : candidate;
            ItemReport found = report.getType() == ReportType.FOUND ? report : candidate;

            MatchEvaluation eval = evaluate(lost, found);

            // Minimum confidence threshold of 55%
            if (eval.score >= 55.0) {
                Optional<ItemMatch> existingMatch = itemMatchRepository.findByLostItemIdAndFoundItemId(lost.getId(), found.getId());
                ItemMatch match;
                if (existingMatch.isPresent()) {
                    match = existingMatch.get();
                    match.setConfidenceScore(eval.score);
                    match.setMatchNotes(eval.explanation);
                } else {
                    match = ItemMatch.builder()
                            .lostItem(lost)
                            .foundItem(found)
                            .confidenceScore(eval.score)
                            .status(MatchStatus.POTENTIAL)
                            .matchNotes(eval.explanation)
                            .build();
                }
                itemMatchRepository.save(match);

                // Update confidence note on reports if not present or lower
                if (lost.getAiConfidenceNote() == null || lost.getAiConfidenceNote().isBlank()) {
                    lost.setAiConfidenceNote(eval.explanation);
                    itemReportRepository.save(lost);
                }
                if (found.getAiConfidenceNote() == null || found.getAiConfidenceNote().isBlank()) {
                    found.setAiConfidenceNote(eval.explanation);
                    itemReportRepository.save(found);
                }
                log.info("AI match detected between #{} ({}) and #{} ({}): score={}%",
                        lost.getId(), lost.getTitle(), found.getId(), found.getTitle(), eval.score);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ItemMatchDto> getMatchesForItem(Long itemId) {
        List<ItemMatch> lostMatches = itemMatchRepository.findByLostItemId(itemId);
        List<ItemMatch> foundMatches = itemMatchRepository.findByFoundItemId(itemId);

        Map<Long, ItemMatch> uniqueMatches = new LinkedHashMap<>();
        for (ItemMatch m : lostMatches) {
            uniqueMatches.put(m.getId(), m);
        }
        for (ItemMatch m : foundMatches) {
            uniqueMatches.put(m.getId(), m);
        }

        return uniqueMatches.values().stream()
                .sorted((a, b) -> Double.compare(b.getConfidenceScore(), a.getConfidenceScore()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ItemMatchDto mapToDto(ItemMatch match) {
        return ItemMatchDto.builder()
                .id(match.getId())
                .lostItemId(match.getLostItem() != null ? match.getLostItem().getId() : null)
                .lostItemTitle(match.getLostItem() != null ? match.getLostItem().getTitle() : null)
                .foundItemId(match.getFoundItem() != null ? match.getFoundItem().getId() : null)
                .foundItemTitle(match.getFoundItem() != null ? match.getFoundItem().getTitle() : null)
                .confidenceScore(match.getConfidenceScore())
                .status(match.getStatus())
                .matchNotes(match.getMatchNotes())
                .createdAt(match.getCreatedAt())
                .build();
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+"))
                .map(String::trim)
                .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }

    public static class MatchEvaluation {
        public final double score;
        public final String explanation;

        public MatchEvaluation(double score, String explanation) {
            this.score = score;
            this.explanation = explanation;
        }
    }
}
