package com.example.bookexchange.common.swagger.page_data_response;

import com.example.bookexchange.book.dto.BookDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BookPageData")
public class BookPageData extends PageResponse<BookDTO> {

}
