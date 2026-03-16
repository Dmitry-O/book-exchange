package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserAdminDTO extends UserDTO {

    @JsonProperty("meta")
    private MetaDTO meta;
}
