package com.example.querysence.model.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnsStatistics {

    private String tableName;

    private String columnName;

    private long rowCount;

    private double distinctCount;

    private double nullFraction;
    @Builder.Default
    private List<String> mostCommonValues = new ArrayList<>();

    @Builder.Default
    private List<Double> mostCommonFrequencies = new ArrayList<>();
}
