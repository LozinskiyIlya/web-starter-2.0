package com.starter.web.controller;


import com.starter.common.exception.Exceptions;
import com.starter.domain.repository.BillRepository;
import com.starter.web.dto.BillDto;
import com.starter.web.mapper.BillMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/bill")
@Schema(title = "Bill-related requests")
public class BillController {

    private final BillRepository billRepository;
    private final BillMapper billMapper;

    @GetMapping("/{billId}")
    public BillDto getBill(@PathVariable UUID billId) {
        return billRepository.findById(billId)
                .map(billMapper::toDto)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
    }
}
