package com.starter.web.service.bill;

import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.User;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.web.dto.BillDto;
import com.starter.web.dto.GroupDto;
import com.starter.web.mapper.BillMapper;
import com.starter.web.mapper.GroupMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final CurrentUserService currentUserService;
    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;
    private final BillRepository billRepository;
    private final BillMapper billMapper;


    public List<GroupDto> getGroups() {
        return currentUserService.getUser()
                .stream()
                .map(groupRepository::findAllByOwner)
                .flatMap(List::stream)
                .map(groupMapper::toDto)
                .toList();
    }

    @Transactional
    public List<String> getGroupMembers(UUID groupId) {
        return groupRepository.findById(groupId)
                .filter(this::hasAccessToViewGroup)
                .stream()
                .map(Group::getMembers)
                .flatMap(List::stream)
                .map(User::getLogin)
                .toList();
    }

    @Transactional
    public List<BillDto> getGroupBills(UUID groupId) {
        return groupRepository.findById(groupId)
                .filter(this::hasAccessToViewGroup)
                .stream()
                .map(billRepository::findAllByGroupOrderByMentionedDateDesc)
                .flatMap(List::stream)
                .map(billMapper::toDto)
                .toList();
    }

    @Transactional
    public GroupDto getGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .filter(this::hasAccessToViewGroup)
                .map(groupMapper::toDto)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
    }

    @Transactional
    public void updateDefaultCurrency(UUID groupId, String currency) {
        final var group = groupRepository.findById(groupId).orElseThrow(Exceptions.ResourceNotFoundException::new);
        currentUserService.getUser()
                .map(User::getId)
                .filter(group.getOwner().getId()::equals)
                .ifPresentOrElse(user -> {
                    group.setDefaultCurrency(currency);
                    groupRepository.save(group);
                }, () -> {
                    throw new Exceptions.WrongUserException();
                });
    }

    private boolean hasAccessToViewGroup(Group group) {
        return currentUserService.getUser()
                .filter(group::contains)
                .map(user -> true)
                .orElseThrow(Exceptions.WrongUserException::new);
    }
}
