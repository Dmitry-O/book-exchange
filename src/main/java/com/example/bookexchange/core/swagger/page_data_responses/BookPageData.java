package com.example.bookexchange.core.swagger.page_data_responses;

import com.example.bookexchange.dto.BookDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BookPageData")
public class BookPageData extends PageResponse<BookDTO> {

}
