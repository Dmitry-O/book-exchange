package com.example.bookexchange.core.swagger.page_data_responses;

import com.example.bookexchange.dto.BookAdminDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BookAdminPageData")
public class BookAdminPageData extends PageResponse<BookAdminDTO> {

}
