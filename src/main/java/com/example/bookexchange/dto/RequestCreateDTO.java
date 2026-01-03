package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RequestCreateDTO.RequestCreateDTOBuilder.class)
@Builder
@Data
public class RequestCreateDTO {

    @JsonProperty("senderUserId")
    @NotNull
    private Long senderUserId;

    @JsonProperty("receiverUserId")
    @NotNull
    private Long receiverUserId;

    @JsonProperty("senderBookId")
    @NotNull
    private Long senderBookId;

    @JsonProperty("receiverBookId")
    @NotNull
    private Long receiverBookId;
}
