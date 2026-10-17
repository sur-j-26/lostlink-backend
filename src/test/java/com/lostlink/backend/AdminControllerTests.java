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
class AdminControllerTests {

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
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User studentUser;
    private User staffUser;
    private ItemReport testReport;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        studentUser = userRepository.findByEmail("admin.test.student@anurag.edu.in").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("admin.test.student@anurag.edu.in")
                        .fullName("Student User")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.STUDENT)
                        .enabled(true)
                        .build())
        );

        staffUser = userRepository.findByEmail("admin.test.staff@anurag.edu.in").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("admin.test.staff@anurag.edu.in")
                        .fullName("Staff Admin User")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.STAFF)
                        .enabled(true)
                        .build())
        );

        Category cat = categoryRepository.findByName("Electronics")
                .orElseGet(() -> categoryRepository.save(new Category(null, "Electronics", "Devices")));

        testReport = itemReportRepository.save(ItemReport.builder()
                .title("Admin Test Tablet")
                .type(ReportType.FOUND)
                .category(cat)
                .location("Campus Lab")
                .description("Found tablet")
                .itemDate(LocalDate.now())
                .status(ItemStatus.REPORTED)
                .verifiedBySecurity(false)
                .build());
    }

    @Test
    @DisplayName("GET /api/admin/stats - Should reject student with 403 Forbidden")
    void testAdminStatsForbiddenForStudent() throws Exception {
        String studentToken = tokenProvider.generateToken(studentUser);

        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/stats - Should return database metrics for authorized STAFF")
    void testAdminStatsSuccessForStaff() throws Exception {
        String staffToken = tokenProvider.generateToken(staffUser);

        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalReports", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalUsers", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/admin/users - Should return registered users list for STAFF")
    void testAdminGetUsers() throws Exception {
        String staffToken = tokenProvider.generateToken(staffUser);

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/admin/reports - Should return all campus reports for STAFF")
    void testAdminGetReports() throws Exception {
        String staffToken = tokenProvider.generateToken(staffUser);

        mockMvc.perform(get("/api/admin/reports")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("PATCH /api/admin/reports/{id}/status - Should update report status and verification by STAFF")
    void testAdminUpdateReportStatus() throws Exception {
        String staffToken = tokenProvider.generateToken(staffUser);
        String updateJson = """
            {
                "status": "HELD_AT_DESK",
                "verifiedBySecurity": true
            }
            """;

        mockMvc.perform(patch("/api/admin/reports/" + testReport.getId() + "/status")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("Held at Helpdesk"))
                .andExpect(jsonPath("$.data.securityVerified").value(true));
    }
}
