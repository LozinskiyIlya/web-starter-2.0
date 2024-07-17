package com.starter.web.controller;


import com.starter.domain.entity.Bill_;
import com.starter.web.dto.BillDto;
import com.starter.web.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/archive")
@Schema(title = "Manage archived bills")
public class ArchiveController {

    private final ArchiveService archiveService;

    @GetMapping("/{groupId}")
    @Operation(summary = "Skipped bills", description = "Get skipped bills by group")
    public Page<BillDto> getBills(
            @PathVariable UUID groupId,
            @PageableDefault(sort = Bill_.CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return archiveService.getBills(groupId, pageable);
    }

    @PostMapping("/{billId}")
    @Operation(summary = "Restore bill", description = "Restore bill by id")
    public void restoreBill(@PathVariable UUID billId) {
        archiveService.restoreBill(billId);
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "Delete all bills", description = "Delete all bills by group")
    public void deleteAllBills(@PathVariable UUID groupId) {
        archiveService.deleteAllBills(groupId);
    }
}
