package com.lostlink.backend;

import com.lostlink.backend.entity.Category;
import com.lostlink.backend.entity.ItemReport;
import com.lostlink.backend.entity.ItemStatus;
import com.lostlink.backend.entity.ReportType;
import com.lostlink.backend.entity.Role;
import com.lostlink.backend.entity.User;
import com.lostlink.backend.repository.CategoryRepository;
import com.lostlink.backend.repository.ItemReportRepository;
import com.lostlink.backend.repository.UserRepository;
import com.lostlink.backend.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class ItemReportControllerTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ItemReportRepository itemReportRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /api/items - Should return seeded campus items successfully")
    void testGetItems() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data[0].type").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/items - Should filter by type=lost")
    void testGetItemsFiltered() throws Exception {
        mockMvc.perform(get("/api/items").param("type", "lost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].type", everyItem(is("lost"))));
    }

    @Test
    @DisplayName("POST /api/items - Should allow campus report creation")
    void testCreateItemReport() throws Exception {
        String itemJson = """
            {
                "title": "TI-84 Plus Calculator Test",
                "type": "lost",
                "category": "Electronics",
                "location": "Math Hall Room 101",
                "date": "2026-10-10",
                "description": "Black calculator misplaced during morning seminar",
                "contactInfo": "student.calc@campus.edu"
            }
            """;

        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("TI-84 Plus Calculator Test"))
                .andExpect(jsonPath("$.data.type").value("lost"));
    }

    @Test
    @DisplayName("PUT /api/items/{id} - Should reject unauthenticated updates with 401")
    void testUpdateItemRequiresAuth() throws Exception {
        mockMvc.perform(put("/api/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated Title\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/items/{id} - Should reject unauthorized user with 403 Forbidden")
    void testUpdateItemForbiddenForDifferentUser() throws Exception {
        // Create reporter user
        User owner = userRepository.findByEmail("kavya.reddy@anurag.edu.in").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("kavya.reddy@anurag.edu.in")
                        .fullName("Kavya Reddy")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.STUDENT)
                        .enabled(true)
                        .build())
        );

        // Create other user
        User otherStudent = userRepository.save(User.builder()
                .email("other.student@anurag.edu.in")
                .fullName("Other Student")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.STUDENT)
                .enabled(true)
                .build());

        Category category = categoryRepository.findByName("Electronics")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Electronics", "Devices")));

        ItemReport report = itemReportRepository.save(ItemReport.builder()
                .title("Owner Report")
                .type(ReportType.LOST)
                .category(category)
                .location("Block A")
                .description("Owner's lost item")
                .itemDate(LocalDate.now())
                .reporter(owner)
                .status(ItemStatus.REPORTED)
                .build());

        String otherToken = tokenProvider.generateToken(otherStudent);

        mockMvc.perform(put("/api/items/" + report.getId())
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Tampered Title\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/items/{id} - Should allow owner to update their own report")
    void testUpdateItemSuccessByOwner() throws Exception {
        User owner = userRepository.findByEmail("kavya.reddy@anurag.edu.in").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("kavya.reddy@anurag.edu.in")
                        .fullName("Kavya Reddy")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.STUDENT)
                        .enabled(true)
                        .build())
        );

        Category category = categoryRepository.findByName("Electronics")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Electronics", "Devices")));

        ItemReport report = itemReportRepository.save(ItemReport.builder()
                .title("Original Title")
                .type(ReportType.LOST)
                .category(category)
                .location("Block A Room 101")
                .description("Original description")
                .itemDate(LocalDate.now())
                .reporter(owner)
                .status(ItemStatus.REPORTED)
                .build());

        String ownerToken = tokenProvider.generateToken(owner);

        mockMvc.perform(put("/api/items/" + report.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated Title by Owner\",\"location\":\"Block B Room 202\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title by Owner"))
                .andExpect(jsonPath("$.data.location").value("Block B Room 202"));
    }

    @Test
    @DisplayName("DELETE /api/items/{id} - Should reject unauthorized user with 403 Forbidden")
    void testDeleteItemForbiddenForDifferentUser() throws Exception {
        User owner = userRepository.findByEmail("kavya.reddy@anurag.edu.in").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("kavya.reddy@anurag.edu.in")
                        .fullName("Kavya Reddy")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.STUDENT)
                        .enabled(true)
                        .build())
        );

        User otherStudent = userRepository.save(User.builder()
                .email("intruder.student@anurag.edu.in")
                .fullName("Intruder Student")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.STUDENT)
                .enabled(true)
                .build());

        Category category = categoryRepository.findByName("Electronics")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Electronics", "Devices")));

        ItemReport report = itemReportRepository.save(ItemReport.builder()
                .title("Protected Report")
                .type(ReportType.LOST)
                .category(category)
                .location("Block C")
                .description("Protected item")
                .itemDate(LocalDate.now())
                .reporter(owner)
                .status(ItemStatus.REPORTED)
                .build());

        String otherToken = tokenProvider.generateToken(otherStudent);

        mockMvc.perform(delete("/api/items/" + report.getId())
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/items/{id} - Should allow owner to delete their report")
    void testDeleteItemSuccessByOwner() throws Exception {
        User owner = userRepository.findByEmail("kavya.reddy@anurag.edu.in").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("kavya.reddy@anurag.edu.in")
                        .fullName("Kavya Reddy")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.STUDENT)
                        .enabled(true)
                        .build())
        );

        Category category = categoryRepository.findByName("Electronics")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Electronics", "Devices")));

        ItemReport report = itemReportRepository.save(ItemReport.builder()
                .title("Item To Delete")
                .type(ReportType.LOST)
                .category(category)
                .location("Block D")
                .description("To be deleted")
                .itemDate(LocalDate.now())
                .reporter(owner)
                .status(ItemStatus.REPORTED)
                .build());

        String ownerToken = tokenProvider.generateToken(owner);

        mockMvc.perform(delete("/api/items/" + report.getId())
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
