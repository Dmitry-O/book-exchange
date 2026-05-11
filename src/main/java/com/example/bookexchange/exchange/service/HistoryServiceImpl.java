package com.example.bookexchange.exchange.service;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.notification.UserUpdate;
import com.example.bookexchange.common.notification.UserUpdateRepository;
import com.example.bookexchange.common.notification.UserUpdateType;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateQueryDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateReadStateChangeDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateReadStateDTO;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.exchange.util.ExchangeReadStateUtil;
import com.example.bookexchange.exchange.util.ExchangeUtil;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final ExchangeRepository exchangeRepository;
    private final UserUpdateRepository userUpdateRepository;
    private final ExchangeUtil exchangeUtil;
    private final ExchangeMapper exchangeMapper;
    private final UserRepository userRepository;
    private final SoftDeleteFilterHelper softDeleteFilterHelper;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeHistoryDTO>> getUserExchangeHistory(Long userId, PageQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageIndex(),
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "updateCreatedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<ExchangeHistoryDTO> page = exchangeRepository
                .findUserExchangeHistory(userId, ExchangeStatus.PENDING, pageable)
                .map(exchange -> exchangeMapper.exchangeToExchangeHistoryDto(
                        exchange,
                        resolveUserExchangeRole(exchange, userId)
                ));

        return ResultFactory.ok(page);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeUpdateDTO>> getExchangeUpdates(Long userId, ExchangeUpdateQueryDTO queryDTO) {
        int pageIndex = queryDTO.getPageIndex();
        int pageSize = queryDTO.getPageSize();
        int fetchLimit = Math.max(1, (pageIndex + 1) * pageSize);
        Pageable exchangeSourcePageable = PageRequest.of(
                0,
                fetchLimit,
                Sort.by(Sort.Direction.DESC, "updateCreatedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Pageable notificationSourcePageable = PageRequest.of(
                0,
                fetchLimit,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Pageable responsePageable = PageRequest.of(pageIndex, pageSize);

        Page<Exchange> exchangePage = nonNullExchangePage(
                loadExchangeUpdates(userId, queryDTO.getReadState(), exchangeSourcePageable),
                exchangeSourcePageable
        );
        Page<UserUpdate> notificationPage = nonNullUserUpdatePage(
                loadNotificationUpdates(userId, queryDTO.getReadState(), notificationSourcePageable),
                notificationSourcePageable
        );

        List<ExchangeUpdateDTO> combined = Stream.concat(
                        exchangePage.getContent().stream().map(exchange -> mapExchangeUpdate(exchange, userId)),
                        notificationPage.getContent().stream().map(this::mapNotificationUpdate)
                )
                .filter(Objects::nonNull)
                .sorted(updateComparator())
                .skip((long) pageIndex * pageSize)
                .limit(pageSize)
                .toList();

        long total = exchangePage.getTotalElements() + notificationPage.getTotalElements();

        return ResultFactory.ok(new PageImpl<>(combined, responsePageable, total));
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeUpdateDTO>> getUnreadExchangeUpdates(Long userId, PageQueryDTO queryDTO) {
        ExchangeUpdateQueryDTO updatesQueryDTO = new ExchangeUpdateQueryDTO();
        updatesQueryDTO.setPageIndex(queryDTO.getPageIndex());
        updatesQueryDTO.setPageSize(queryDTO.getPageSize());
        updatesQueryDTO.setReadState(ExchangeUpdateReadStateDTO.UNREAD);

        return getExchangeUpdates(userId, updatesQueryDTO);
    }

    @Transactional
    @Override
    public Result<ExchangeUpdateDTO> updateExchangeUpdateReadState(Long userId, Long exchangeId, ExchangeUpdateReadStateChangeDTO dto) {
        return exchangeUtil.identifyUserExchangeRole(userId, exchangeId)
                .flatMap(userRole -> ResultFactory.fromRepository(
                                exchangeRepository,
                                exchangeId,
                                MessageKey.EXCHANGE_NOT_FOUND
                        )
                        .map(exchange -> updateExchangeReadState(exchange, userRole, dto.getIsRead()))
                        .map(exchange -> {
                            ExchangeUpdateDTO updateDto = exchangeMapper.exchangeToExchangeUpdateDto(exchange, userRole);
                            if (updateDto != null && updateDto.getUpdateType() == null) {
                                updateDto.setUpdateType(UserUpdateType.EXCHANGE);
                            }
                            return updateDto;
                        })
                );
    }

    @Transactional
    @Override
    public Result<ExchangeUpdateDTO> updateNotificationUpdateReadState(Long userId, Long notificationId, ExchangeUpdateReadStateChangeDTO dto) {
        return ResultFactory.fromOptional(
                        userUpdateRepository.findByIdAndUserId(notificationId, userId),
                        MessageKey.EXCHANGE_NOT_FOUND
                )
                .map(update -> updateNotificationReadState(update, dto.getIsRead()))
                .map(this::mapNotificationUpdate);
    }

    @Transactional
    @Override
    public Result<Void> markAllUpdatesAsRead(Long userId) {
        exchangeRepository.markAllSenderUpdatesAsRead(userId);
        exchangeRepository.markAllReceiverUpdatesAsRead(userId);
        userUpdateRepository.markAllAsRead(userId);

        return ResultFactory.ok((Void) null);
    }

    @Transactional
    @Override
    public Result<ExchangeHistoryDetailsDTO> getUserExchangeHistoryDetails(Long userId, Long exchangeId) {
        return exchangeUtil.identifyUserExchangeRole(userId, exchangeId)
                .flatMap(userRole ->
                        ResultFactory.fromRepository(
                                exchangeRepository,
                                exchangeId,
                                MessageKey.EXCHANGE_NOT_FOUND
                        )
                        .map(exchange -> markExchangeAsRead(exchange, userRole))
                        .map(exchange -> toExchangeHistoryDetailsDto(exchange, userRole))
                );
    }

    private Exchange markExchangeAsRead(Exchange exchange, UserExchangeRole userRole) {
        return switch (userRole) {
            case SENDER -> markAsReadBySender(exchange);
            case RECEIVER -> markAsReadByReceiver(exchange);
        };
    }

    private Exchange updateExchangeReadState(Exchange exchange, UserExchangeRole userRole, boolean isRead) {
        boolean currentReadState = resolveReadState(exchange, userRole);

        if (currentReadState == isRead) {
            return exchange;
        }

        if (userRole == UserExchangeRole.SENDER) {
            if (isRead) {
                ExchangeReadStateUtil.markReadBySender(exchange);
            } else {
                ExchangeReadStateUtil.markUnreadBySender(exchange);
            }
        } else {
            if (isRead) {
                ExchangeReadStateUtil.markReadByReceiver(exchange);
            } else {
                ExchangeReadStateUtil.markUnreadByReceiver(exchange);
            }
        }

        return exchangeRepository.saveAndFlush(exchange);
    }

    private UserExchangeRole resolveUserExchangeRole(Exchange exchange, Long userId) {
        return exchange.getSenderUser().getId().equals(userId)
                ? UserExchangeRole.SENDER
                : UserExchangeRole.RECEIVER;
    }

    private Page<Exchange> loadExchangeUpdates(Long userId, ExchangeUpdateReadStateDTO readState, Pageable pageable) {
        if (readState == null || readState == ExchangeUpdateReadStateDTO.ALL) {
            return exchangeRepository.findUpdatesForUser(userId, pageable);
        }

        if (readState == ExchangeUpdateReadStateDTO.READ) {
            return exchangeRepository.findUpdatesForUserByReadState(userId, true, pageable);
        }

        return exchangeRepository.findUpdatesForUserByReadState(userId, false, pageable);
    }

    private Page<UserUpdate> loadNotificationUpdates(Long userId, ExchangeUpdateReadStateDTO readState, Pageable pageable) {
        if (readState == null || readState == ExchangeUpdateReadStateDTO.ALL) {
            return userUpdateRepository.findForUser(userId, pageable);
        }

        if (readState == ExchangeUpdateReadStateDTO.READ) {
            return userUpdateRepository.findForUserByReadState(userId, true, pageable);
        }

        return userUpdateRepository.findForUserByReadState(userId, false, pageable);
    }

    private ExchangeUpdateDTO mapExchangeUpdate(Exchange exchange, Long userId) {
        ExchangeUpdateDTO dto = exchangeMapper.exchangeToExchangeUpdateDto(
                exchange,
                resolveUserExchangeRole(exchange, userId)
        );

        if (dto != null && dto.getUpdateType() == null) {
            dto.setUpdateType(UserUpdateType.EXCHANGE);
        }

        return dto;
    }

    private ExchangeUpdateDTO mapNotificationUpdate(UserUpdate update) {
        ExchangeUpdateDTO dto = new ExchangeUpdateDTO();
        dto.setId(update.getId());
        dto.setNotificationId(update.getId());
        dto.setUpdateType(update.getType());
        dto.setIsRead(Boolean.TRUE.equals(update.getIsRead()));
        dto.setTargetUrl(update.getTargetUrl());
        dto.setUpdateCreatedAt(update.getCreatedAt());
        dto.setBook(toUpdateBook(update));
        dto.setTargetUserId(update.getTargetUserId());
        dto.setReportId(update.getReportId());
        dto.setReportTargetType(update.getReportTargetType());
        dto.setReportReason(update.getReportReason());
        dto.setReportStatus(update.getReportStatus());
        dto.setTargetUserNickname(resolveNotificationTargetUserNickname(update));
        dto.setTargetUserPhotoUrl(resolveNotificationTargetUserPhotoUrl(update));

        return dto;
    }

    private String resolveNotificationTargetUserNickname(UserUpdate update) {
        User currentTargetUser = resolveDeletedReportTargetUser(update);

        if (currentTargetUser != null) {
            return currentTargetUser.getNickname();
        }

        return update.getTargetUserNickname();
    }

    private String resolveNotificationTargetUserPhotoUrl(UserUpdate update) {
        User currentTargetUser = resolveDeletedReportTargetUser(update);

        if (currentTargetUser != null) {
            return null;
        }

        return update.getTargetUserPhotoUrl();
    }

    private User resolveDeletedReportTargetUser(UserUpdate update) {
        if (update.getReportTargetType() != TargetType.USER || update.getTargetUserId() == null) {
            return null;
        }

        User currentTargetUser = softDeleteFilterHelper.runWithoutDeletedFilter(() ->
                userRepository.findById(update.getTargetUserId()).orElse(null)
        );

        if (currentTargetUser == null || currentTargetUser.getDeletedAt() == null) {
            return null;
        }

        return currentTargetUser;
    }

    private com.example.bookexchange.exchange.dto.ExchangeUpdateBookDTO toUpdateBook(UserUpdate update) {
        if (update.getBookId() == null && update.getBookName() == null && update.getBookPhotoUrl() == null) {
            return null;
        }

        com.example.bookexchange.exchange.dto.ExchangeUpdateBookDTO book = new com.example.bookexchange.exchange.dto.ExchangeUpdateBookDTO();
        book.setId(update.getBookId());
        book.setName(update.getBookName());
        book.setPhotoUrl(update.getBookPhotoUrl());
        book.setIsGift(update.getBookGift());
        return book;
    }

    private UserUpdate updateNotificationReadState(UserUpdate update, boolean isRead) {
        if (Boolean.TRUE.equals(update.getIsRead()) == isRead) {
            return update;
        }

        update.setIsRead(isRead);
        return userUpdateRepository.saveAndFlush(update);
    }

    private Comparator<ExchangeUpdateDTO> updateComparator() {
        return Comparator
                .comparing(
                        ExchangeUpdateDTO::getUpdateCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .reversed()
                .thenComparing(
                        update -> update.getNotificationId() != null ? update.getNotificationId() : update.getExchangeId(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
    }

    private Page<Exchange> nonNullExchangePage(Page<Exchange> page, Pageable pageable) {
        return page != null ? page : Page.empty(pageable);
    }

    private Page<UserUpdate> nonNullUserUpdatePage(Page<UserUpdate> page, Pageable pageable) {
        return page != null ? page : Page.empty(pageable);
    }

    private boolean resolveReadState(Exchange exchange, UserExchangeRole userRole) {
        return userRole == UserExchangeRole.SENDER
                ? Boolean.TRUE.equals(exchange.getIsReadBySender())
                : Boolean.TRUE.equals(exchange.getIsReadByReceiver());
    }

    private Exchange markAsReadBySender(Exchange exchange) {
        if (!exchange.getIsReadBySender()) {
            ExchangeReadStateUtil.markReadBySender(exchange);

            return exchangeRepository.saveAndFlush(exchange);
        }

        return exchange;
    }

    private Exchange markAsReadByReceiver(Exchange exchange) {
        if (!exchange.getIsReadByReceiver()) {
            ExchangeReadStateUtil.markReadByReceiver(exchange);

            return exchangeRepository.saveAndFlush(exchange);
        }

        return exchange;
    }

    private ExchangeHistoryDetailsDTO toExchangeHistoryDetailsDto(Exchange exchange, UserExchangeRole userRole) {
        return switch (userRole) {
            case SENDER -> exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                    exchange,
                    exchange.getReceiverUser().getId(),
                    exchange.getReceiverUser().getNickname(),
                    resolveContactDetails(exchange, userRole),
                    userRole
            );
            case RECEIVER -> exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                    exchange,
                    exchange.getSenderUser().getId(),
                    exchange.getSenderUser().getNickname(),
                    resolveContactDetails(exchange, userRole),
                    userRole
            );
        };
    }

    private String resolveContactDetails(Exchange exchange, UserExchangeRole userRole) {
        if (exchange.getStatus() != ExchangeStatus.APPROVED) {
            return null;
        }

        return switch (userRole) {
            case SENDER -> exchange.getReceiverBook().getContactDetails();
            case RECEIVER -> resolveContactDetails(exchange.getSenderBook());
        };
    }

    private String resolveContactDetails(Book book) {
        return book != null ? book.getContactDetails() : null;
    }
}
