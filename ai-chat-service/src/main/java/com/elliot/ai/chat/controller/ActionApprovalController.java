package com.elliot.ai.chat.controller;

import com.elliot.ai.chat.dto.ApiResponse;
import com.elliot.ai.chat.service.ActionApprovalService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/actions")
public class ActionApprovalController {

    private final ActionApprovalService actionApprovalService;

    public ActionApprovalController(ActionApprovalService actionApprovalService) {
        this.actionApprovalService = actionApprovalService;
    }

    @PostMapping("/{actionId}/confirm")
    public ApiResponse<String> confirm(@PathVariable String actionId) {
        actionApprovalService.confirmApproval(actionId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{actionId}/reject")
    public ApiResponse<String> reject(@PathVariable String actionId) {
        actionApprovalService.rejectApproval(actionId);
        return ApiResponse.success(null);
    }
}