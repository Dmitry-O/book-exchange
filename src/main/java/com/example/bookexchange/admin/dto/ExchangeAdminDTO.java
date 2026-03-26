package com.example.bookexchange.admin.dto;

import com.example.bookexchange.common.audit.dto.EntityAuditMetadataDTO;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ExchangeAdminDTO {

    @Schema(example = "1")
    @JsonProperty("id")
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

    @Schema(example = "PENDING")
    @JsonProperty("status")
    private ExchangeStatus status;

    @Schema(example = "true")
    @JsonProperty("isReadByReceiver")
    private Boolean isReadByReceiver;

    @Schema(example = "false")
    @JsonProperty("isReadBySender")
    private Boolean isReadBySender;

    @JsonProperty("meta")
    private EntityAuditMetadataDTO meta;
}
