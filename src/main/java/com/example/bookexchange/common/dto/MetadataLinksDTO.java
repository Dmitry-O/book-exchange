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
public class MetadataLinksDTO {

    @Schema(example = "https://github.com/your-username/book-exchange-backend")
    @JsonProperty("backendGithubUrl")
    private String backendGithubUrl;

    @Schema(example = "https://github.com/your-username/book-exchange-frontend")
    @JsonProperty("frontendGithubUrl")
    private String frontendGithubUrl;

    @Schema(example = "https://www.linkedin.com/in/your-name")
    @JsonProperty("linkedinUrl")
    private String linkedinUrl;

    @Schema(example = "https://api.example.com/swagger-ui/index.html")
    @JsonProperty("swaggerUrl")
    private String swaggerUrl;
}
