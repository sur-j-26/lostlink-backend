package com.lostlink.backend;

import com.lostlink.backend.entity.*;
import com.lostlink.backend.repository.CategoryRepository;
import com.lostlink.backend.repository.ClaimRepository;
import com.lostlink.backend.repository.ItemReportRepository;
import com.lostlink.backend.repository.UserRepository;
import com.lostlink.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ClaimControllerTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemReportRepository itemReportRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User student1;
    private User student2;
    private User staffUser;
    private ItemReport foundItem;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        student1 = userRepository.findByEmail("claim.student1@anurag.edu.in").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("claim.student1@anurag.edu.in")
                        .fullName("Student One")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.STUDENT)
                        .enabled(true)
                        .build())
        );

        student2 = userRepository.findByEmail("claim.student2@anurag.edu.in").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("claim.student2@anurag.edu.in")
                        .fullName("Student Two")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.STUDENT)
                        .enabled(true)
                        .build())
        );

        staffUser = userRepository.findByEmail("security.staff@anurag.edu.in").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("security.staff@anurag.edu.in")
                        .fullName("Security Staff")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.STAFF)
                        .enabled(true)
                        .build())
        );

        Category electronics = categoryRepository.findByName("Electronics")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Electronics", "Devices")));

        foundItem = itemReportRepository.save(ItemReport.builder()
                .title("Scientific Calculator")
                .type(ReportType.FOUND)
                .category(electronics)
                .location("Block A Rm 101")
                .itemDate(LocalDate.now())
                .description("Casio scientific calculator found on desk")
                .status(ItemStatus.HELD_AT_DESK)
                .reporter(student1)
                .build());
    }

    @Test
    @DisplayName("POST /api/claims - Should reject unauthenticated claim submission with 401")
    void testSubmitClaimRequiresAuth() throws Exception {
        String claimJson = String.format("""
            {
                "itemId": %d,
                "verificationNotes": "It has my name written in pencil on back cover"
            }
            """, foundItem.getId());

        mockMvc.perform(post("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .content(claimJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/claims - Should reject reporter claiming their own item with 400")
    void testReporterCannotClaimOwnItem() throws Exception {
        String token = tokenProvider.generateToken(student1);
        String claimJson = String.format("""
            {
                "itemId": %d,
                "verificationNotes": "I want to claim my own reported item"
            }
            """, foundItem.getId());

        mockMvc.perform(post("/api/claims")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(claimJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot submit an ownership claim on an item you reported yourself")));
    }

    @Test
    @DisplayName("POST /api/claims - Should successfully submit claim by student2")
    void testSubmitClaimSuccess() throws Exception {
        String token = tokenProvider.generateToken(student2);
        String claimJson = String.format("""
            {
                "itemId": %d,
                "verificationNotes": "Serial number #CASIO-9921 and scratched cover"
            }
            """, foundItem.getId());

        mockMvc.perform(post("/api/claims")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(claimJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.claimantName").value("Student Two"))
                .andExpect(jsonPath("$.data.itemId").value(foundItem.getId()));
    }

    @Test
    @DisplayName("POST /api/claims - Should reject duplicate claim submission for same item")
    void testSubmitDuplicateClaimRejected() throws Exception {
        String token = tokenProvider.generateToken(student2);
        String claimJson = String.format("""
            {
                "itemId": %d,
                "verificationNotes": "First claim submission"
            }
            """, foundItem.getId());

        mockMvc.perform(post("/api/claims")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(claimJson))
                .andExpect(status().isCreated());

        // Second submission should be rejected
        mockMvc.perform(post("/api/claims")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(claimJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already submitted an active claim")));
    }

    @Test
    @DisplayName("GET /api/claims/my - Should return submitted claims for current user")
    void testGetMyClaims() throws Exception {
        String token = tokenProvider.generateToken(student2);
        claimRepository.save(Claim.builder()
                .item(foundItem)
                .claimant(student2)
                .verificationNotes("Identifying marks test")
                .status(ClaimStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/claims/my")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].verificationNotes").value("Identifying marks test"));
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/review - Staff can approve claim, transitioning claim to APPROVED and item to CLAIMED")
    void testReviewClaimApprovedByStaff() throws Exception {
        Claim claim = claimRepository.save(Claim.builder()
                .item(foundItem)
                .claimant(student2)
                .verificationNotes("Valid ownership description")
                .status(ClaimStatus.PENDING)
                .build());

        String staffToken = tokenProvider.generateToken(staffUser);
        String reviewJson = """
            {
                "status": "APPROVED",
                "feedback": "Student verified at security desk Rm 104 with Anurag University ID"
            }
            """;

        mockMvc.perform(put("/api/claims/" + claim.getId() + "/review")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.adminFeedback").value("Student verified at security desk Rm 104 with Anurag University ID"));

        // Verify item status in database updated to CLAIMED
        ItemReport updatedItem = itemReportRepository.findById(foundItem.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ItemStatus.CLAIMED, updatedItem.getStatus());
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/review - Staff can reject claim with feedback")
    void testReviewClaimRejectedByStaff() throws Exception {
        Claim claim = claimRepository.save(Claim.builder()
                .item(foundItem)
                .claimant(student2)
                .verificationNotes("Insufficient proof")
                .status(ClaimStatus.PENDING)
                .build());

        String staffToken = tokenProvider.generateToken(staffUser);
        String reviewJson = """
            {
                "status": "REJECTED",
                "feedback": "Serial number did not match item custody record"
            }
            """;

        mockMvc.perform(put("/api/claims/" + claim.getId() + "/review")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.adminFeedback").value("Serial number did not match item custody record"));
    }
}
