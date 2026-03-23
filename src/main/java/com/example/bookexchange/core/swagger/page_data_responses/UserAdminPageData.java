package com.example.bookexchange.core.swagger.page_data_responses;

import com.example.bookexchange.dto.UserAdminDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserAdminPageData")
public class UserAdminPageData extends PageResponse<UserAdminDTO> {

}
