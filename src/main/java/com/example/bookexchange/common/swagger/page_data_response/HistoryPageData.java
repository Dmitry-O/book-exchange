package com.example.bookexchange.common.swagger.page_data_response;

import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HistoryPageData")
public class HistoryPageData extends PageResponse<ExchangeHistoryDTO> {

}
