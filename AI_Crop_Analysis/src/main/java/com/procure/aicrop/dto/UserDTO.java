package com.procure.aicrop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.procure.aicrop.entity.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String state;
    private String district;
    private Double latitude;
    private Double longitude;
    private String address;
    private Long areaAcres;
    private User.UserRole role;
    private Boolean active;

    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .state(user.getState())
                .district(user.getDistrict())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .address(user.getAddress())
                .areaAcres(user.getAreaAcres())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }
}
