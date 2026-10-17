package com.lostlink.backend.runner;

import com.lostlink.backend.entity.Role;
import com.lostlink.backend.entity.User;
import com.lostlink.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Runner that provisions or conditionally resets the test account password.
 * Only active in non-production profiles (!prod).
 *
 * To prevent unexpected password overwrites on restart, password updates for
 * an existing test account only occur when 'dev.reset-test-password-on-startup=true'.
 */
@Component
@Profile("!prod")
public class TestAccountPasswordResetRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestAccountPasswordResetRunner.class);
    private static final String TEST_EMAIL = "test.student@campus.edu";
    private static final String ANURAG_TEST_EMAIL = "kavya.reddy@anurag.edu.in";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${dev.test-password:Password123!}")
    private String defaultPassword;

    @Value("${dev.reset-test-password-on-startup:false}")
    private boolean resetOnStartup;

    public TestAccountPasswordResetRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        provisionOrUpdateTestAccount(ANURAG_TEST_EMAIL, "Kavya Reddy", "21U1A0505", Role.STUDENT);
        provisionOrUpdateTestAccount(TEST_EMAIL, "Test Student", "STU-2026-001", Role.STUDENT);
    }

    private void provisionOrUpdateTestAccount(String email, String fullName, String campusId, Role role) {
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            boolean passwordMismatch = !passwordEncoder.matches(defaultPassword, user.getPassword());
            if (resetOnStartup || passwordMismatch) {
                user.setPassword(passwordEncoder.encode(defaultPassword));
                userRepository.save(user);
                log.info("Test account {} password has been safely synchronized to the configured test password.", email);
            } else {
                log.info("Test account {} already exists with verified credentials.", email);
            }
        } else {
            User newUser = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .campusId(campusId)
                    .role(role)
                    .enabled(true)
                    .password(passwordEncoder.encode(defaultPassword))
                    .build();
            userRepository.save(newUser);
            log.info("Development test account has been provisioned for: {}", email);
        }
    }
}
