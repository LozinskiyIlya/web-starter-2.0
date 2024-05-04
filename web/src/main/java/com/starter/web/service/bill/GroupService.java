package com.starter.web.service.bill;

import com.starter.common.exception.Exceptions;
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

    private final GroupRepository groupRepository;
    private final BillRepository billRepository;
    private final GroupMapper groupMapper;

    @Transactional
    public GroupDto getGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .map(group -> groupMapper.toDto(group, billRepository.findAllByGroupOrderByMentionedDateDesc(group)))
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
    }
}
