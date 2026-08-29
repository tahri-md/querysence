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
public class TestConnectionResponse {

    private boolean success;
    private String status;      // CONNECTED, FAILED, EXPIRED_CREDENTIALS
    private String message;     // human-readable detail, e.g. driver error
    private Long latencyMs;
}
