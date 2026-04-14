package com.example.bookexchange.common.api;

import com.example.bookexchange.book.dto.BookCategoryDTO;
import com.example.bookexchange.book.dto.BookSortFieldDTO;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.common.dto.MetadataDTO;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.user.dto.SupportedLocalesDTO;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.model.UserType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Tag(name = "Metadata")
@RestController
@RequiredArgsConstructor
public class MetadataController {

    private final ResultResponseMapper responseMapper;

    @ApiResponse(
            responseCode = "200",
            description = "Metadata for frontend forms and filters",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MetadataDTO.class)
            )
    )
    @GetMapping(MetadataPaths.METADATA_PATH)
    public ResponseEntity<?> getMetadata(HttpServletRequest request) {
        MetadataDTO metadata = MetadataDTO.builder()
                .locales(Arrays.stream(SupportedLocalesDTO.values())
                        .map(SupportedLocalesDTO::getProperty)
                        .toList())
                .reportReasons(Arrays.stream(ReportReason.values())
                        .map(Enum::name)
                        .toList())
                .reportStatuses(Arrays.stream(ReportStatus.values())
                        .map(Enum::name)
                        .toList())
                .exchangeStatuses(Arrays.stream(ExchangeStatus.values())
                        .map(Enum::name)
                        .toList())
                .bookTypes(Arrays.stream(BookType.values())
                        .map(Enum::name)
                        .toList())
                .userTypes(Arrays.stream(UserType.values())
                        .map(Enum::name)
                        .toList())
                .roles(Arrays.stream(UserRole.values())
                        .map(Enum::name)
                        .toList())
                .bookSortFields(Arrays.stream(BookSortFieldDTO.values())
                        .map(Enum::name)
                        .toList())
                .bookCategories(Arrays.stream(BookCategoryDTO.values())
                        .map(BookCategoryDTO::getProperty)
                        .toList())
                .build();

        return responseMapper.map(ResultFactory.ok(metadata), request);
    }
}
