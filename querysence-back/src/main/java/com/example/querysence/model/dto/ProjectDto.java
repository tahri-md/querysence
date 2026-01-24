package com.example.querysence.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



import com.example.querysence.model.SchemaDefinition;
import com.example.querysence.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ProjectDto {

    private String name;

    private String description;

    private User owner;
    @Builder.Default
    private List<SchemaDefinition> schemas = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
