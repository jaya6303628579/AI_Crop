package com.procure.aicrop.controller;

import com.procure.aicrop.dto.ApiResponse;
import com.procure.aicrop.dto.AuthRequest;
import com.procure.aicrop.dto.AuthResponse;
import com.procure.aicrop.dto.UserDTO;
import com.procure.aicrop.dto.UserRegistrationRequest;
import com.procure.aicrop.service.AuthService;
import com.procure.aicrop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            log.info("Registration attempt for email: {}", request.getEmail());
            log.info("Request details - Full Name: {}, Phone: {}", request.getFullName(), request.getPhoneNumber());

            AuthResponse response = authService.register(request);

            log.info("Registration successful for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User registered successfully", response));
        } catch (RuntimeException e) {
            log.error("Registration failed for email: {} - Error: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("REGISTRATION_FAILED", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("REGISTRATION_ERROR", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        try {
            log.info("JWT login attempt for email: {}", request.getEmail());
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (RuntimeException e) {
            log.error("JWT login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("LOGIN_FAILED", "Invalid email or password"));
        }
    }

    @PostMapping("/login-form")
    public ResponseEntity<ApiResponse<AuthResponse>> loginForm(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String password,
            HttpServletResponse response) {
        try {
            log.info("LOGIN ATTEMPT - Email: {}", email);
            log.debug("Password received, length: {}", password != null ? password.length() : "null");

            // Validate inputs
            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                log.warn("LOGIN FAILED - Missing email or password");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("LOGIN_FAILED", "Email and password are required"));
            }

            String trimmedEmail = email.trim();
            log.info("Email trimmed: {}", trimmedEmail);

            // Find user by email
            com.procure.aicrop.entity.User user = userService.findByEmail(trimmedEmail)
                    .orElseThrow(() -> {
                        log.warn("LOGIN FAILED - User not found with email: '{}'", trimmedEmail);
                        return new RuntimeException("User not found");
                    });

            log.info("User found in database - ID: {}, Email: {}, Active: {}",
                    user.getId(), user.getEmail(), user.getActive());

            // Check if user is active
            if (!user.getActive()) {
                log.warn("LOGIN FAILED - User account is inactive: {}", trimmedEmail);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("LOGIN_FAILED", "User account is inactive"));
            }

            log.info("User is active, proceeding with password validation");

            // Verify password with BCrypt comparison
            log.info("Validating BCrypt password");
            log.debug("Password from form - length: {}", password.length());
            log.debug("Hash from database - starts with: {}...",
                    user.getPassword().substring(0, Math.min(20, user.getPassword().length())));

            boolean passwordValid = authService.validatePassword(password, user.getPassword());

            log.info("Password validation result: {}", passwordValid);

            if (!passwordValid) {
                log.warn("LOGIN FAILED - BCrypt password validation returned FALSE for: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("LOGIN_FAILED", "Invalid email or password"));
            }

            log.info("Password validation PASSED");

            // Generate JWT token (7 days expiration)
            String token = authService.generateToken(trimmedEmail);
            log.info("JWT token generated successfully");

            // Set token as HTTP-only cookie
            response.addCookie(createTokenCookie(token, 7 * 24 * 60 * 60));
            log.info("Token set as HTTP-only cookie");

            // Return user data + token
            AuthResponse authResponse = AuthResponse.builder()
                    .token(token)
                    .user(com.procure.aicrop.dto.UserDTO.fromEntity(user))
                    .message("Login successful")
                    .build();

            log.info("LOGIN SUCCESSFUL - User: {} (ID: {})", trimmedEmail, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));

        } catch (Exception e) {
            log.error("LOGIN EXCEPTION - {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("LOGIN_FAILED", "Invalid email or password"));
        }
    }

    private jakarta.servlet.http.Cookie createTokenCookie(String token, int maxAgeSeconds) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("Authorization", "Bearer " + token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.info("Logout request for user: {}", auth.getName());
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "User logged out"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        boolean isValid = authService.validateToken(jwtToken);
        return ResponseEntity.ok(ApiResponse.success("Token validated", isValid));
    }

    @GetMapping("/current-user")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        try {
            UserDTO currentUser = userService.getCurrentUserDTO();
            log.info("Current user fetched: {} (ID: {})", currentUser.getEmail(), currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Current user", currentUser));
        } catch (RuntimeException e) {
            log.warn("Current user not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("NOT_AUTHENTICATED", "No user logged in"));
        }
    }
}
