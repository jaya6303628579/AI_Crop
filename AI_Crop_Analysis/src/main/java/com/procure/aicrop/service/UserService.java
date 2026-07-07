package com.procure.aicrop.service;

import com.procure.aicrop.dto.UserDTO;
import com.procure.aicrop.dto.UserRegistrationRequest;
import com.procure.aicrop.entity.User;
import com.procure.aicrop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .state(request.getState())
                .district(request.getDistrict())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .areaAcres(request.getAreaAcres())
                .role(User.UserRole.FARMER)
                .active(true)
                .build();

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public UserDTO getUserDTO(Long id) {
        return userRepository.findById(id)
                .map(UserDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUser(Long id, UserDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getDistrict() != null) user.setDistrict(request.getDistrict());
        if (request.getLatitude() != null) user.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) user.setLongitude(request.getLongitude());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getAreaAcres() != null) user.setAreaAcres(request.getAreaAcres());

        return userRepository.save(user);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }
}
