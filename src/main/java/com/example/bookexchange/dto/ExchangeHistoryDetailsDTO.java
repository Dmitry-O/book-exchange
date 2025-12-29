package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.UserExchangeRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExchangeHistoryDetailsDTO {

    @NotNull
    private Long id;

    @NotBlank
    @NotNull
    @Size(min = 5, max = 20)
    private String userNickname;

    @NotNull
    private ExchangeStatus status;

    private BookDTO senderBook;

    private BookDTO receiverBook;

    @NotBlank
    @NotNull
    private String contactDetails;

    @NotBlank
    @NotNull
    private UserExchangeRole userExchangeRole;
}
