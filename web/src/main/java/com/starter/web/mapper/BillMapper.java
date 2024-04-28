package com.starter.web.mapper;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.web.dto.BillDto;
import com.starter.web.dto.BillDto.BillTagDto;
import org.mapstruct.Mapper;

@Mapper
public interface BillMapper {

    BillDto toDto(Bill bill);

    BillTagDto toTagDto(BillTag tag);
}
