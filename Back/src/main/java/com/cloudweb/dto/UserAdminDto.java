package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAdminDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime date;
    private Integer failedLoginAttempts;
    private String statutsUser;
    private String userType;
}
