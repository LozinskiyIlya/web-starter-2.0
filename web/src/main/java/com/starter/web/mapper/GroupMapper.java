package com.starter.web.mapper;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.User;
import com.starter.web.dto.GroupDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {BillMapper.class})
public interface GroupMapper {

    GroupDto toDto(Group group, List<Bill> bills);

    default String toUserName(User user) {
        return user.getUserInfo().getFullName();
    }
}
