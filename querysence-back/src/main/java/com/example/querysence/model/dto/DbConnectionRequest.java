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
public class DbConnectionRequest {

    private String name;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;

    // Plaintext in transit only (over HTTPS); encrypted server-side before storage,
    // never echoed back.
    private String password;

    @Builder.Default
    private String dialect = "POSTGRESQL";

    @Builder.Default
    private Boolean sslEnabled = true;

    @Builder.Default
    private Boolean readOnlyEnforced = true;
}
