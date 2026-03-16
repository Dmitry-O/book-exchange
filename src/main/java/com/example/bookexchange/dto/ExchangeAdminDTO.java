package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExchangeAdminDTO {

    @JsonProperty("id")
    @NotNull
    private Long id;

    @JsonProperty("senderBook")
    private BookAdminDTO senderBook;

    @JsonProperty("receiverBook")
    private BookAdminDTO receiverBook;

    @JsonProperty("senderUser")
    private UserAdminDTO senderUser;

    @JsonProperty("receiverUser")
    private UserAdminDTO receiverUser;

    @JsonProperty("declinerUser")
    private UserAdminDTO declinerUser;

    @JsonProperty("status")
    private ExchangeStatus status;

    @JsonProperty("isReadByReceiver")
    private Boolean isReadByReceiver;

    @JsonProperty("isReadBySender")
    private Boolean isReadBySender;

    @JsonProperty("meta")
    private MetaDTO meta;
}
