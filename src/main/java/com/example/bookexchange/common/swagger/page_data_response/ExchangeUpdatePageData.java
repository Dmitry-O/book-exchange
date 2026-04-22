package com.example.bookexchange.common.swagger.page_data_response;

import com.example.bookexchange.exchange.dto.ExchangeUpdateDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExchangeUpdatePageData")
public class ExchangeUpdatePageData extends PageResponse<ExchangeUpdateDTO> {

}
