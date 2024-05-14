package com.starter.web.mapper;

import com.starter.domain.entity.Group;
import com.starter.domain.repository.BillRepository;
import com.starter.web.dto.GroupDto;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class GroupMapper {

    private final StaticGroupMapper staticMapper;
    private final BillRepository billRepository;

    public GroupDto toDto(Group group){
        return staticMapper.toDto(group, billRepository.countByGroup(group));
    };

    @Mapper(componentModel = "spring")
    interface StaticGroupMapper {

        @Mapping(target ="ownerId", source = "group.owner.id")
        GroupDto toDto(Group group, long billsCount);
    }
}



