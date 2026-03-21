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
public class ProjectMemberDto {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private ProjectRole role;
    private LocalDateTime joinedAt;
}
