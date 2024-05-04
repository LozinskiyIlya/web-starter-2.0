package com.starter.web.service.bill;

import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.web.dto.GroupDto;
import com.starter.web.mapper.GroupMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final CurrentUserService currentUserService;
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;
    private final GroupMapper groupMapper;

    @Transactional
    public GroupDto getGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .filter(this::canView)
                .map(group -> groupMapper.toDto(group, billRepository.findAllByGroupOrderByMentionedDateDesc(group)))
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
    }

    private boolean canView(Group group) {
        return currentUserService.getUser()
                .filter(group::contains)
                .map(user -> true)
                .orElseThrow(Exceptions.WrongUserException::new);
    }
}
