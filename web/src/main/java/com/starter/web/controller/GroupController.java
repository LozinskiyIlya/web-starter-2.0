package com.starter.web.controller;

import com.starter.web.dto.GroupDto;
import com.starter.web.service.bill.GroupService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/groups")
@Schema(title = "Group-related requests")
public class GroupController {

    private final GroupService groupService;

    @GetMapping("/{groupId}")
    public GroupDto getGroup(@PathVariable UUID groupId) {
        return groupService.getGroup(groupId);
    }
}
