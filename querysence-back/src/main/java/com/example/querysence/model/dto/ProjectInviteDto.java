package com.example.querysence.model.dto;

import com.example.querysence.model.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInviteDto {
    private Long id;
    private String inviteCode;
    private String email;
    private ProjectRole role;
    private Long projectId;
    private String createdByEmail;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Boolean isUsed;
}
