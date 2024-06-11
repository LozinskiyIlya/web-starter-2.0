package com.starter.web.service;


import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Bill;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.web.dto.BillDto;
import com.starter.web.mapper.BillMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final CurrentUserService currentUserService;
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;
    private final BillMapper billMapper;

    @Transactional
    public Page<BillDto> getBills(UUID groupId, Pageable pageable) {
        final var currentUser = currentUserService.getUser().orElseThrow();
        final var group = groupRepository.findById(groupId)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new Exceptions.WrongUserException("You can't view bills in this group");
        }
        return billRepository.findAllByGroupAndStatusEquals(group, Bill.BillStatus.SKIPPED, pageable)
                .map(billMapper::toDto);
    }

    @Transactional
    public void restoreBill(UUID billId) {
        final var currentUser = currentUserService.getUser().orElseThrow();
        final var bill = billRepository.findById(billId)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
        if (!bill.getGroup().getOwner().getId().equals(currentUser.getId())) {
            throw new Exceptions.WrongUserException("You can't restore this bill");
        }
        bill.setStatus(Bill.BillStatus.CONFIRMED);
        billRepository.saveAndFlush(bill);
    }

    @Transactional
    public void deleteAllBills(UUID groupId) {
        final var currentUser = currentUserService.getUser().orElseThrow();
        final var group = groupRepository.findById(groupId)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new Exceptions.WrongUserException("You can't delete bills in this group");
        }
        billRepository.deleteAllByGroupAndStatus(group, Bill.BillStatus.SKIPPED);
        billRepository.flush();
    }
}
