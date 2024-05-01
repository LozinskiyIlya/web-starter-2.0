package com.starter.web.mapper;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.web.dto.BillDto;
import com.starter.web.dto.BillDto.BillTagDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.LinkedHashSet;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface BillMapper {

    @Mapping(target = "date", source = "mentionedDate")
    @Mapping(target= "groupTitle", source = "group.title")
    BillDto toDto(Bill bill);

    BillTagDto toTagDto(BillTag tag);

    @Mapping(target = "mentionedDate", source = "date")
    @Mapping(target = "tags", expression = "java(toTagEntities(billDto.getTags()))")
    Bill updateEntityFromDto(BillDto billDto, @MappingTarget Bill bill);

    default Set<BillTag> toTagEntities(Set<BillTagDto> tags) {
        return new LinkedHashSet<>(tags.stream().map(this::toTagEntity).toList());
    }

    BillTag toTagEntity(BillTagDto dto);
}
