package com.starter.web.mapper;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.web.dto.GroupDto;
import com.starter.web.dto.GroupDto.GroupLastBillDto;
import com.starter.web.dto.GroupDto.GroupMemberDto;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class GroupMapper {

    private final StaticGroupMapper staticMapper;
    private final BillRepository billRepository;
    private final GroupRepository groupRepository;

    public GroupDto toDto(Group group) {
        return staticMapper.toDto(
                group,
                billRepository.findFirstByGroupOrderByMentionedDateDesc(group),
                billRepository.countByGroup(group),
                groupRepository.countMembers(group)
        );
    }

    public GroupMemberDto toGroupMemberDto(UserInfo userInfo) {
        return staticMapper.toGroupMemberDto(userInfo);
    }

    @Mapper(componentModel = "spring")
    interface StaticGroupMapper {

        @Mapping(target = "ownerId", source = "group.owner.id")
        @Mapping(target = "id", source = "group.id")
        GroupDto toDto(Group group, Bill lastBill, long billsCount, long membersCount);

        @Mapping(target = "name", expression = "java(userInfo.getFullName())")
        @Mapping(target = "id", source = "userInfo.user.id")
        GroupMemberDto toGroupMemberDto(UserInfo userInfo);

        @Mapping(target = "date", source = "mentionedDate")
        GroupLastBillDto toLastBillDto(Bill lastBill);
    }
}



