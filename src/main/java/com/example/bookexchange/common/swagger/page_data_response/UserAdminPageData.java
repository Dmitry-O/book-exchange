package com.example.bookexchange.common.swagger.page_data_response;

import com.example.bookexchange.admin.dto.UserAdminDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserAdminPageData")
public class UserAdminPageData extends PageResponse<UserAdminDTO> {

}
