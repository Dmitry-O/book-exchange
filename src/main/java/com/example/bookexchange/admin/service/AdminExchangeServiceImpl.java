package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.ExchangeAdminDTO;
import com.example.bookexchange.admin.mapper.AdminMapper;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.util.ETagUtil;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminExchangeServiceImpl implements AdminExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final AdminMapper adminMapper;
    private final AuditService auditService;

    @Override
    public Result<Page<ExchangeAdminDTO>> findExchanges(PageQueryDTO queryDTO, Set<ExchangeStatus> exchangeStatuses) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageIndex(),
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<Exchange> pendingExchangesPage;

        if (exchangeStatuses != null) {
            pendingExchangesPage = exchangeRepository.findByStatusIn(exchangeStatuses, pageable);
        } else {
            pendingExchangesPage = exchangeRepository.findAll(pageable);
        }

        return ResultFactory.ok(
                pendingExchangesPage.map(adminMapper::exchangeToExchangeAdminDto)
        );
    }

    @Override
    public Result<ExchangeAdminDTO> findExchangeById(UserDetails adminUser, Long exchangeId) {
        return ResultFactory.fromRepository(
                        exchangeRepository,
                        exchangeId,
                        MessageKey.ADMIN_EXCHANGE_NOT_FOUND
                )
                .flatMap(exchange -> {
                            auditService.log(AuditEvent.builder()
                                    .action("ADMIN_EXCHANGE_FIND")
                                    .result(AuditResult.SUCCESS)
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("exchangeId", exchangeId)
                                    .build()
                            );

                            return ResultFactory.okETag(
                                    adminMapper.exchangeToExchangeAdminDto(exchange),
                                    ETagUtil.form(exchange)
                            );
                        }
                );
    }
}
