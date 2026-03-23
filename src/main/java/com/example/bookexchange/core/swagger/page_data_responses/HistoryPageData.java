package com.example.bookexchange.core.swagger.page_data_responses;

import com.example.bookexchange.dto.ExchangeHistoryDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HistoryPageData")
public class HistoryPageData extends PageResponse<ExchangeHistoryDTO> {

}
