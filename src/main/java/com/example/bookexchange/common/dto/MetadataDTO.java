package com.example.bookexchange.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetadataDTO {

    @Schema(example = "[\"en\", \"de\", \"ru\"]")
    @JsonProperty("locales")
    private List<String> locales;

    @Schema(example = "[\"SPAM\", \"INAPPROPRIATE\", \"FRAUD\", \"OTHER\"]")
    @JsonProperty("reportReasons")
    private List<String> reportReasons;

    @Schema(example = "[\"OPEN\", \"RESOLVED\", \"REJECTED\"]")
    @JsonProperty("reportStatuses")
    private List<String> reportStatuses;

    @Schema(example = "[\"PENDING\", \"APPROVED\", \"DECLINED\"]")
    @JsonProperty("exchangeStatuses")
    private List<String> exchangeStatuses;

    @Schema(example = "[\"ACTIVE\", \"DELETED\", \"ALL\"]")
    @JsonProperty("bookTypes")
    private List<String> bookTypes;

    @Schema(example = "[\"ACTIVE\", \"DELETED\", \"ALL\"]")
    @JsonProperty("userTypes")
    private List<String> userTypes;

    @Schema(example = "[\"USER\", \"ADMIN\", \"SUPER_ADMIN\"]")
    @JsonProperty("roles")
    private List<String> roles;

    @Schema(example = "[\"NAME\", \"AUTHOR\", \"CATEGORY\", \"PUBLICATION_YEAR\", \"CITY\", \"CREATED_AT\", \"UPDATED_AT\"]")
    @JsonProperty("bookSortFields")
    private List<String> bookSortFields;

    @Schema(example = "[\"Drama\", \"Fantasy\", \"Science Fiction\", \"History\", \"Other\"]")
    @JsonProperty("bookCategories")
    private List<String> bookCategories;
}
