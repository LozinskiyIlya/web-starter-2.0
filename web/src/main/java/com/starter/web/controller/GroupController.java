package com.starter.web.controller;

import com.starter.web.dto.BillDto;
import com.starter.web.dto.GroupDto;
import com.starter.web.service.bill.GroupService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.starter.web.dto.GroupDto.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/groups")
@Schema(title = "Group-related requests")
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public List<GroupDto> getGroups(){
        return groupService.getGroups();
    }

    @GetMapping("/{groupId}")
    public GroupDto getGroup(@PathVariable UUID groupId) {
        return groupService.getGroup(groupId);
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMemberDto> getGroupMembers(@PathVariable UUID groupId) {
        return groupService.getGroupMembers(groupId);
    }

    @GetMapping("/{groupId}/bills")
    public List<BillDto> getGroupBills(@PathVariable UUID groupId) {
        return groupService.getGroupBills(groupId);
    }

    @PostMapping("/{groupId}/currency")
    public void updateDefaultCurrency(@PathVariable UUID groupId, @RequestBody @Valid UpdateDefaultCurrencyRequest request) {
        groupService.updateDefaultCurrency(groupId, request.getCurrency());
    }


    @Data
    public static class UpdateDefaultCurrencyRequest {
        @Schema(description = "New default currency")
        @NotBlank
        private String currency;
    }
}
