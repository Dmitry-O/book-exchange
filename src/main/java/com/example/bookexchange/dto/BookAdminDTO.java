package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BookAdminDTO extends BookDTO {

    @JsonProperty("meta")
    private MetaDTO meta;
}
