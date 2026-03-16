package com.example.bookexchange.mappers;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.*;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AdminMapper {

    MetaDTO metaToMetaDto(SoftDeletableEntity entity);

    @Mapping(target = "meta", source = ".")
    BookAdminDTO bookToBookAdminDto(Book book);

    @Mapping(target = "meta", source = ".")
    UserAdminDTO userToUserAdminDto(User user);

    @Mapping(target = "meta", source = ".")
    ExchangeAdminDTO exchangeToExchangeAdminDto(Exchange exchange);

    @Mapping(target = "meta", source = ".")
    ReportAdminDTO reportToReportAdminDto(Report report);
}
