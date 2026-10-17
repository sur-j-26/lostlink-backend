package com.lostlink.backend;

import com.lostlink.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean temporary test users if present
        userRepository.findByEmail("student@anurag.edu.in").ifPresent(userRepository::delete);
        userRepository.findByEmail("faculty.member@anurag.edu.in").ifPresent(userRepository::delete);
        userRepository.findByEmail("temp.user@anurag.edu.in").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("POST /api/auth/register - Should register Anurag University student successfully and return JWT")
    void testRegisterSuccess() throws Exception {
        String registerJson = """
            {
                "fullName": "Anurag Student",
                "email": "student@anurag.edu.in",
                "password": "Password123!",
                "studentId": "21U1A0501",
                "role": "STUDENT"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("student@anurag.edu.in"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist()); // Verify password hash is NEVER exposed
    }

    @Test
    @DisplayName("POST /api/auth/register - Should handle case-insensitive Anurag University email")
    void testRegisterCaseInsensitiveDomain() throws Exception {
        String registerJson = """
            {
                "fullName": "Faculty Member",
                "email": "FACULTY.MEMBER@ANURAG.EDU.IN",
                "password": "Password123!",
                "studentId": "FAC-2026-001",
                "role": "FACULTY"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("faculty.member@anurag.edu.in"));
    }

    @ParameterizedTest(name = "Reject invalid registration email: {0}")
    @ValueSource(strings = {
            "student@gmail.com",
            "student@yahoo.com",
            "student@outlook.com",
            "student@otheruniversity.edu.in",
            "anurag.edu.in",
            "student@subdomain.anurag.edu.in",
            "student@anurag.edu.in.example.com",
            "@anurag.edu.in",
            "invalid-email-address"
    })
    @DisplayName("POST /api/auth/register - Should reject non-Anurag or malformed emails with 400 Bad Request")
    void testRegisterRejectInvalidDomains(String invalidEmail) throws Exception {
        String registerJson = String.format("""
            {
                "fullName": "Test Applicant",
                "email": "%s",
                "password": "Password123!",
                "studentId": "21U1A0599",
                "role": "STUDENT"
            }
            """, invalidEmail);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should reject duplicate email registration with 400")
    void testRegisterDuplicateEmail() throws Exception {
        String registerJson = """
            {
                "fullName": "Anurag Student",
                "email": "student@anurag.edu.in",
                "password": "Password123!",
                "studentId": "21U1A0501",
                "role": "STUDENT"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should reject self-selected ADMIN role")
    void testRegisterAdminRejected() throws Exception {
        String adminRegisterJson = """
            {
                "fullName": "Malicious Admin Attempt",
                "email": "admin.attempt@anurag.edu.in",
                "password": "Password123!",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(adminRegisterJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Administrator accounts cannot be created via public registration")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should authenticate registered Anurag account with valid credentials")
    void testLoginSuccess() throws Exception {
        String registerJson = """
            {
                "fullName": "Login Student",
                "email": "temp.user@anurag.edu.in",
                "password": "Password123!",
                "studentId": "21U1A0502",
                "role": "STUDENT"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isCreated());

        String loginJson = """
            {
                "email": "temp.user@anurag.edu.in",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("temp.user@anurag.edu.in"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 with invalid password")
    void testLoginInvalidPassword() throws Exception {
        String loginJson = """
            {
                "email": "nonexistent@anurag.edu.in",
                "password": "WrongPassword999!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("GET /api/auth/me - Should return current user with valid Bearer token")
    void testGetCurrentUserWithToken() throws Exception {
        String registerJson = """
            {
                "fullName": "Profile User",
                "email": "temp.user@anurag.edu.in",
                "password": "Password123!",
                "role": "STUDENT"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        org.json.JSONObject json = new org.json.JSONObject(responseBody);
        String token = json.getJSONObject("data").getString("token");

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("temp.user@anurag.edu.in"))
                .andExpect(jsonPath("$.data.fullName").value("Profile User"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test Account - Verify password reset and login for test.student@campus.edu")
    void testLoginWithResetTestAccount() throws Exception {
        // Step 1: Reset password using dev endpoint
        String resetJson = """
            {
                "newPassword": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/reset-test-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 2: Login with test.student@campus.edu and Password123!
        String loginJson = """
            {
                "email": "test.student@campus.edu",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("test.student@campus.edu"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());

        // Step 3: Verify wrong password returns 401 Unauthorized
        String wrongPasswordJson = """
            {
                "email": "test.student@campus.edu",
                "password": "WrongPassword!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongPasswordJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("Dev Account - Reset endpoint must reject external IP addresses")
    void testResetEndpointForbiddenForExternalIp() throws Exception {
        String resetJson = """
            {
                "newPassword": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/reset-test-password")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.25");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("only permitted from local development environments")));
    }

    @Test
    @DisplayName("Anurag Campus Test Account - Verify login for kavya.reddy@anurag.edu.in returns valid JWT and STUDENT role")
    void testLoginKavyaReddyStudentAccount() throws Exception {
        String loginJson = """
            {
                "email": "kavya.reddy@anurag.edu.in",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("kavya.reddy@anurag.edu.in"))
                .andExpect(jsonPath("$.data.user.fullName").value("Kavya Reddy"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }
}
