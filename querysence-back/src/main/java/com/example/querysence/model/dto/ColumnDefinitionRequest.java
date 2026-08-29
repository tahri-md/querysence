package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinitionRequest {

    private String columnName;

    private String dataType;

    @Builder.Default
    private Boolean isNullable = true;

    @Builder.Default
    private Boolean isPrimaryKey = false;

    @Builder.Default
    private Boolean isForeignKey = false;

    private String referencesTable;

    private String referencesColumn;

}