package com.starter.web.mapper;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.User;
import com.starter.domain.repository.BillRepository;
import com.starter.web.dto.GroupDto;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GroupMapper {

    private final StaticGroupMapper staticMapper;
    private final BillRepository billRepository;

    public GroupDto toDto(Group group){
        final var bills = billRepository.findAllByGroupOrderByMentionedDateDesc(group);
        return staticMapper.toDto(group, bills);
    };


    @Mapper(componentModel = "spring", uses = {BillMapper.class})
    interface StaticGroupMapper {
        GroupDto toDto(Group group, List<Bill> bills);

        default String toUserName(User user) {
            return user.getUserInfo().getFullName();
        }
    }
}



