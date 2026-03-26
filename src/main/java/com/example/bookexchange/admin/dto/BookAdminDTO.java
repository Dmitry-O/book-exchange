package com.example.bookexchange.admin.dto;

import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.common.audit.dto.EntityAuditMetadataDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BookAdminDTO extends BookDTO {

    @JsonProperty("meta")
    private EntityAuditMetadataDTO meta;
}
