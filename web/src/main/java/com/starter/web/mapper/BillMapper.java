package com.starter.web.mapper;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.domain.entity.Group;
import com.starter.web.dto.BillDto;
import com.starter.web.dto.BillDto.BillTagDto;
import com.starter.web.fragments.BillAssistantResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.LinkedHashSet;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {GroupMapper.class}, imports = {Bill.BillStatus.class})
public interface BillMapper {

    @Mapping(target = "date", source = "mentionedDate")
    BillDto toDto(Bill bill);

    BillTagDto toTagDto(BillTag tag);

    BillTag toTagEntity(BillTagDto dto);

    @Mapping(target = "mentionedDate", source = "date")
    @Mapping(target = "tags", expression = "java(toTagEntities(billDto.getTags()))")
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    Bill updateEntityFromDto(BillDto billDto, @MappingTarget Bill bill);

    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "group", source = "group")
    @Mapping(target = "status", expression = "java(BillStatus.NEW)")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Bill fromModelResponse(BillAssistantResponse response, Group group, Set<BillTag> tags);

    default Set<BillTag> toTagEntities(Set<BillTagDto> tags) {
        return new LinkedHashSet<>(tags.stream().map(this::toTagEntity).toList());
    }
}
