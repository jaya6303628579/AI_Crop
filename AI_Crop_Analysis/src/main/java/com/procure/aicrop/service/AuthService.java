package com.procure.aicrop.service;

import com.procure.aicrop.dto.AuthRequest;
import com.procure.aicrop.dto.AuthResponse;
import com.procure.aicrop.dto.UserDTO;
import com.procure.aicrop.dto.UserRegistrationRequest;
import com.procure.aicrop.entity.User;
import com.procure.aicrop.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(UserRegistrationRequest request) {
        User user = userService.registerUser(request);
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(UserDTO.fromEntity(user))
                .message("User registered successfully")
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        Optional<User> userOptional = userService.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!user.getActive()) {
            throw new RuntimeException("User account is inactive");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(UserDTO.fromEntity(user))
                .message("Login successful")
                .build();
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    public String getUserEmailFromToken(String token) {
        return jwtTokenProvider.getEmailFromToken(token);
    }
}
