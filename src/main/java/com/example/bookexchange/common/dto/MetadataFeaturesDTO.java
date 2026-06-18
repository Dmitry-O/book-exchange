package com.example.bookexchange.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetadataFeaturesDTO {

    @Schema(
            description = "Whether the browser-accessible demo email sandbox is available",
            example = "true"
    )
    @JsonProperty("demoEmailSandboxEnabled")
    private boolean demoEmailSandboxEnabled;
}
