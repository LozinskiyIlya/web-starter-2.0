package com.starter.web.mapper;


import com.starter.domain.entity.UserSettings;
import com.starter.web.dto.UserSettingsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Instant;

@Mapper(componentModel = "spring", imports = {Instant.class})
public interface UserSettingsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "lastUpdatedAt", expression = "java(Instant.now())")
    UserSettings updateEntityFromDto(UserSettingsDto dto, @MappingTarget UserSettings settings);


    @Mapping(target = "dailyReminderAt", source = "dailyReminderAt", dateFormat = "HH:mm")
    UserSettingsDto toDto(UserSettings settings);


    @Mapping(target = "pinCode", constant = "******")
    @Mapping(target = "dailyReminderAt", source = "dailyReminderAt", dateFormat = "HH:mm")
    UserSettingsDto toDtoMaskedPin(UserSettings settings);
}
