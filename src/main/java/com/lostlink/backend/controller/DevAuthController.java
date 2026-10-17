package com.lostlink.backend.controller;

import com.lostlink.backend.dto.common.ApiResponse;
import com.lostlink.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Development-only controller for safe test account password reset.
 * Strictly disabled in production profiles (!prod) and restricted to local requests.
 */
@RestController
@RequestMapping("/api/auth")
@Profile("!prod")
@ConditionalOnProperty(name = "dev.auth.test-reset-enabled", havingValue = "true", matchIfMissing = true)
public class DevAuthController {

    private final AuthService authService;
    private final Environment environment;

    @Value("${dev.auth.reset-token:}")
    private String configuredDevResetToken;

    public DevAuthController(AuthService authService, Environment environment) {
        this.authService = authService;
        this.environment = environment;
    }

    @PostMapping("/reset-test-password")
    public ResponseEntity<ApiResponse<Void>> resetTestPassword(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-Dev-Reset-Token", required = false) String requestToken,
            @RequestBody(required = false) Map<String, String> request
    ) {
        // Guard 1: Runtime defense against production exposure
        if (environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Test account password reset is strictly disabled in production."));
        }

        // Guard 2: Restrict strictly to local development calls (localhost / 127.0.0.1 / ::1)
        String remoteAddr = servletRequest != null ? servletRequest.getRemoteAddr() : null;
        if (!isLocalAddress(remoteAddr)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Test account password reset is only permitted from local development environments."));
        }

        // Guard 3: Optional token protection if configured
        if (configuredDevResetToken != null && !configuredDevResetToken.isBlank()) {
            if (requestToken == null || !configuredDevResetToken.equals(requestToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or missing development authorization token (X-Dev-Reset-Token)."));
            }
        }

        String targetEmail = (request != null && request.get("email") != null && !request.get("email").isBlank())
                ? request.get("email").trim()
                : "test.student@campus.edu";

        String newPassword = (request != null && request.get("newPassword") != null && !request.get("newPassword").isBlank())
                ? request.get("newPassword")
                : "Password123!";

        authService.resetTestAccountPassword(targetEmail, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password for " + targetEmail + " has been reset successfully", null));
    }

    private boolean isLocalAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return ip.equals("127.0.0.1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("::1")
                || ip.equalsIgnoreCase("localhost");
    }
}
