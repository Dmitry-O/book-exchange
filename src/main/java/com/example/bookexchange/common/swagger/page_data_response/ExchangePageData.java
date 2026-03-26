package com.example.bookexchange.common.swagger.page_data_response;

import com.example.bookexchange.exchange.dto.ExchangeDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExchangePageData")
public class ExchangePageData extends PageResponse<ExchangeDTO> {

}
