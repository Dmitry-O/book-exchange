package com.example.bookexchange.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RequestCreateDTO {

    @NotNull
    private Long senderUserId;

    @NotNull
    private Long receiverUserId;

    @NotNull
    private Long senderBookId;

    @NotNull
    private Long receiverBookId;
}
