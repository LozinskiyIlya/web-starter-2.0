package com.starter.web.controller;

import com.starter.web.dto.BillDto;
import com.starter.web.dto.GroupDto;
import com.starter.web.service.bill.GroupService;
import com.starter.web.service.bill.GroupService.InsightsDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.starter.web.dto.GroupDto.GroupMemberDto;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/groups")
@Schema(title = "Group-related requests")
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public Page<GroupDto> getGroups(@PageableDefault Pageable pageable) {
        return groupService.getGroups(pageable);
    }

    @GetMapping("/{groupId}")
    public GroupDto getGroup(@PathVariable UUID groupId) {
        return groupService.getGroup(groupId);
    }

    @GetMapping("/{groupId}/insights")
    public InsightsDto getInsights(@PathVariable UUID groupId, @RequestParam(required = false) boolean forceUpdate) {
        return groupService.getInsights(groupId, forceUpdate);
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMemberDto> getGroupMembers(@PathVariable UUID groupId) {
        return groupService.getGroupMembers(groupId);
    }

    @GetMapping("/{groupId}/bills")
    public Page<BillDto> getGroupBills(@PathVariable UUID groupId, @PageableDefault(size = 20) Pageable pageable) {
        return groupService.getGroupBills(groupId, pageable);
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
