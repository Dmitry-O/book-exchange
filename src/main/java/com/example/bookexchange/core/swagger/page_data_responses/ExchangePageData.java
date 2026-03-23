package com.example.bookexchange.core.swagger.page_data_responses;

import com.example.bookexchange.dto.ExchangeDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExchangePageData")
public class ExchangePageData extends PageResponse<ExchangeDTO> {

}
