package com.starter.web.controller;


import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillTagRepository;
import com.starter.web.dto.BillDto;
import com.starter.web.mapper.BillMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/bills")
@Schema(title = "Bill-related requests")
public class BillController {
    private final CurrentUserService currentUserService;
    private final BillRepository billRepository;
    private final BillTagRepository billTagRepository;
    private final BillMapper billMapper;

    @GetMapping("/{billId}")
    public BillDto getBill(@PathVariable UUID billId) {
        return billRepository.findById(billId)
                .map(billMapper::toDto)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
    }

    @PostMapping("/{billId}")
    public void updateBill(@PathVariable UUID billId, @RequestBody @Valid BillDto billDto) {
        final var bill = billRepository.findById(billId)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
        final var updated = billMapper.updateEntityFromDto(billDto, bill);
        updated.setStatus(Bill.BillStatus.CONFIRMED);
        billRepository.save(updated);
    }

    @GetMapping("/tags")
    public List<BillDto.BillTagDto> getTags() {
        final var userTags = currentUserService.getUser()
                .map(billTagRepository::findAllByUser)
                .orElse(Set.of());
        final var defaultTags = billTagRepository.findAllByTagType(BillTag.TagType.DEFAULT);
        return Stream.concat(userTags.stream(), defaultTags.stream())
                .map(billMapper::toTagDto)
                .toList();
    }
}
