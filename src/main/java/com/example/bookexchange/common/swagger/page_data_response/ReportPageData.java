package com.example.bookexchange.common.swagger.page_data_response;

import com.example.bookexchange.report.dto.ReportDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportPageData")
public class ReportPageData extends PageResponse<ReportDTO> {

}
