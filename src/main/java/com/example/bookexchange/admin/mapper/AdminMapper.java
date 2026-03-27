package com.example.bookexchange.admin.mapper;

import com.example.bookexchange.admin.dto.BookAdminDTO;
import com.example.bookexchange.admin.dto.ExchangeAdminDTO;
import com.example.bookexchange.admin.dto.UserAdminDTO;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.dto.EntityAuditMetadataDTO;
import com.example.bookexchange.common.mapper.TemporalMapper;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.common.audit.model.SoftDeletableEntity;
import com.example.bookexchange.user.model.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        uses = TemporalMapper.class
)
public interface AdminMapper {

    EntityAuditMetadataDTO metaToMetaDto(SoftDeletableEntity entity);

    @Mapping(target = "meta", source = ".")
    BookAdminDTO bookToBookAdminDto(Book book);

    @Mapping(target = "meta", source = ".")
    UserAdminDTO userToUserAdminDto(User user);

    @Mapping(target = "meta", source = ".")
    ExchangeAdminDTO exchangeToExchangeAdminDto(Exchange exchange);
}
