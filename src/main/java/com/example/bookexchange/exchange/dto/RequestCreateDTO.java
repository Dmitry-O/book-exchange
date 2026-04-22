package com.example.bookexchange.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RequestCreateDTO.RequestCreateDTOBuilder.class)
@Builder
@Data
public class RequestCreateDTO {

    @Schema(example = "2")
    @JsonProperty("receiverUserId")
    @NotNull
    private Long receiverUserId;

    @Schema(
            example = "1",
            description = "Required for regular exchanges. Optional only when the requested receiver book is marked as a gift."
    )
    @JsonProperty("senderBookId")
    private Long senderBookId;

    @Schema(example = "2")
    @JsonProperty("receiverBookId")
    @NotNull
    private Long receiverBookId;
}
