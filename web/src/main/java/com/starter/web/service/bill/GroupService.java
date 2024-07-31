package com.starter.web.service.bill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.User;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.web.dto.BillDto;
import com.starter.web.dto.GroupDto;
import com.starter.web.dto.GroupDto.GroupMemberDto;
import com.starter.web.mapper.BillMapper;
import com.starter.web.mapper.GroupMapper;
import com.starter.web.service.openai.OpenAiAssistant;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
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
    private final OpenAiAssistant assistant;
    private final ObjectMapper objectMapper;


    public Page<GroupDto> getGroups(Pageable pageable) {
        return currentUserService.getUser()
                .map(u -> groupRepository.findGroupsByOwnerOrderByLatestBill(u, pageable))
                .map(page -> page.map(groupMapper::toDto))
                .orElseThrow();
    }

    @Transactional
    public List<GroupMemberDto> getGroupMembers(UUID groupId) {
        return groupRepository.findById(groupId)
                .filter(this::hasAccessToViewGroup)
                .map(Group::getMembers)
                .orElseThrow(Exceptions.ResourceNotFoundException::new)
                .stream()
                .map(User::getUserInfo)
                .map(groupMapper::toGroupMemberDto)
                .toList();
    }

    @Transactional
    public Page<BillDto> getGroupBills(UUID groupId, Pageable pageable) {
        return groupRepository.findById(groupId)
                .filter(this::hasAccessToViewGroup)
                .map(group -> billRepository.findAllNotSkippedByGroup(group, pageable))
                .map(page -> page.map(billMapper::toDto))
                .orElseThrow(Exceptions.ResourceNotFoundException::new);

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


    @Transactional
    public InsightsDto getInsights(UUID groupId, boolean forceUpdate) {
        final var group = groupRepository.findById(groupId)
                .filter(this::hasAccessToViewGroup)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
        if (StringUtils.hasText(group.getInsights()) && !forceUpdate) {
            return new InsightsDto(group.getInsights(), group.getInsightsUpdatedAt().toString());
        }
        final var insights = requestInsights(group);
        group.setInsights(insights);
        group.setInsightsUpdatedAt(Instant.now());
        groupRepository.save(group);
        return new InsightsDto(insights, group.getInsightsUpdatedAt().toString());
    }

    private boolean hasAccessToViewGroup(Group group) {
        return currentUserService.getUser()
                .filter(group::contains)
                .map(user -> true)
                .orElseThrow(Exceptions.WrongUserException::new);
    }

    @SneakyThrows(Exception.class)
    private String requestInsights(Group group) {
        final var bills = billRepository.findAllNotSkippedByGroup(group, Pageable.ofSize(100))
                .stream()
                .map(billMapper::toDto)
                .toList();
        final var json = objectMapper.writeValueAsString(bills);
        return assistant.getInsights(json);
    }

    public record InsightsDto(String text, String updatedAt) {

    }
}
