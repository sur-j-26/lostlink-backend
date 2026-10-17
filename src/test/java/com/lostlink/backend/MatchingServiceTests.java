package com.lostlink.backend;

import com.lostlink.backend.entity.Category;
import com.lostlink.backend.entity.ItemReport;
import com.lostlink.backend.entity.ReportType;
import com.lostlink.backend.service.MatchingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MatchingServiceTests {

    @Autowired
    private MatchingService matchingService;

    @Test
    @DisplayName("MatchingService - Should detect high match score between similar lost and found items")
    void testEvaluateSimilarItems() {
        Category electronics = new Category(1L, "Electronics", "Devices");

        ItemReport lostLaptop = ItemReport.builder()
                .title("Apple MacBook Air M2 Space Gray")
                .type(ReportType.LOST)
                .category(electronics)
                .location("Central Library Floor 2")
                .description("Silver gray laptop with university sticker misplaced near study area")
                .itemDate(LocalDate.now().minusDays(1))
                .build();

        ItemReport foundChargerAndLaptop = ItemReport.builder()
                .title("MacBook Air Laptop with Power Adapter")
                .type(ReportType.FOUND)
                .category(electronics)
                .location("Library Study Desk Floor 2")
                .description("Found Apple MacBook Air laptop and charger left unattended")
                .itemDate(LocalDate.now())
                .build();

        MatchingService.MatchEvaluation evaluation = matchingService.evaluate(lostLaptop, foundChargerAndLaptop);

        assertNotNull(evaluation);
        assertTrue(evaluation.score >= 55.0, "Score should be >= 55% for matching laptop items: was " + evaluation.score);
        assertTrue(evaluation.explanation.contains("Matching category: Electronics"));
        assertTrue(evaluation.explanation.contains("MacBook") || evaluation.explanation.contains("macbook") || evaluation.explanation.contains("title"));
    }

    @Test
    @DisplayName("MatchingService - Should produce low score for completely unrelated items")
    void testEvaluateUnrelatedItems() {
        Category keys = new Category(2L, "Keys", "Keys");
        Category books = new Category(3L, "Books & Stationery", "Books");

        ItemReport lostKey = ItemReport.builder()
                .title("Dorm Brass Room Key")
                .type(ReportType.LOST)
                .category(keys)
                .location("Hostel Block B")
                .description("Single brass key with blue tag")
                .itemDate(LocalDate.now().minusDays(10))
                .build();

        ItemReport foundBook = ItemReport.builder()
                .title("Organic Chemistry Textbook")
                .type(ReportType.FOUND)
                .category(books)
                .location("Auditorium Hall C")
                .description("Chemistry book highlighted in yellow")
                .itemDate(LocalDate.now())
                .build();

        MatchingService.MatchEvaluation evaluation = matchingService.evaluate(lostKey, foundBook);

        assertNotNull(evaluation);
        assertTrue(evaluation.score < 30.0, "Score should be low for completely different items: was " + evaluation.score);
    }
}
