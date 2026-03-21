package com.example.querysence.model.dto;

import com.example.querysence.model.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInviteRequest {
    private String email;
    private ProjectRole role;
}
