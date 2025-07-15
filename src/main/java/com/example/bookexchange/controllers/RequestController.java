package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.services.RequestService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/request")
public class RequestController {

    private final RequestService requestService;

    @PostMapping()
    public ExchangeDTO createRequest(@RequestBody RequestCreateDTO dto) {
        return requestService.createRequest(dto);
    }

    @GetMapping("/{senderUserId}/{exchangeId}")
    public ExchangeDetailsDTO getUserRequestDetails(@PathVariable("senderUserId") Long senderUserId, @PathVariable("exchangeId") Long exchangeId) {
        return requestService.getSenderRequestDetails(senderUserId, exchangeId);
    }

    @GetMapping("/{senderUserId}")
    public Page<ExchangeDTO> getUserRequests(
            @PathVariable("senderUserId") Long senderUserId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return requestService.getSenderRequests(senderUserId, pageIndex, pageSize);
    }

    @PatchMapping("/decline/{senderUserId}/{exchangeId}")
    public String declineUserRequest(@PathVariable("senderUserId") Long senderUserId, @PathVariable("exchangeId") Long exchangeId) {
        return requestService.declineUserRequest(senderUserId, exchangeId);
    }
}
