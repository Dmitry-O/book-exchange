package com.example.bookexchange.admin.dto;

import com.example.bookexchange.common.audit.dto.EntityAuditMetadataDTO;
import com.example.bookexchange.user.dto.UserDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserAdminDTO extends UserDTO {

    @JsonProperty("meta")
    private EntityAuditMetadataDTO meta;
}
