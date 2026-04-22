package com.example.bookexchange.exchange.dto;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExchangeUpdateQueryDTO extends PageQueryDTO {

    @Schema(description = "Filter updates by read state", example = "ALL")
    @JsonProperty("readState")
    private ExchangeUpdateReadStateDTO readState = ExchangeUpdateReadStateDTO.ALL;
}
