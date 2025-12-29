package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExchangeDetailsDTO {

    @NotNull
    private Long id;

    @NotNull
    private ExchangeStatus exchangeStatus;

    @NotBlank
    @NotNull
    @Size(min = 5, max = 20)
    private String userNickname;

    @NotBlank
    @NotNull
    private BookDTO senderBook;

    @NotBlank
    @NotNull
    private BookDTO receiverBook;
}
