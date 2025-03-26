package com.company.approvalsystem;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    @Autowired
    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approve(@PathVariable UUID id) {
        approvalService.approve(id);
        return ResponseEntity.ok("Time entry approved");
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<String> reject(@PathVariable UUID id, @RequestBody RejectRequest request) {
        approvalService.reject(id, request.getReason());
        return ResponseEntity.ok("Time entry rejected");
    }

    @PostMapping("/{id}/delegate")
    public ResponseEntity<String> delegateApproval(@PathVariable UUID id, @RequestBody DelegateRequest request) {
        approvalService.delegate(id, request.getDelegateTo());
        return ResponseEntity.ok("Approval delegated");
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<String> escalateApproval(@PathVariable UUID id, @RequestBody EscalateRequest request) {
        approvalService.escalate(id, request.getEscalateTo());
        return ResponseEntity.ok("Approval escalated");
    }
}

@Service
class ApprovalService {
    private final Map<UUID, Approval> approvalDatabase = new HashMap<>();

    public void approve(UUID id) {
        Approval approval = getApproval(id);
        approval.setStatus("Approved");
    }

    public void reject(UUID id, String reason) {
        Approval approval = getApproval(id);
        approval.setStatus("Rejected");
        approval.setReason(reason);
    }

    public void delegate(UUID id, UUID delegateTo) {
        Approval approval = getApproval(id);
        approval.setDelegatedTo(delegateTo);
        approval.setStatus("Delegated");
    }

    public void escalate(UUID id, UUID escalateTo) {
        Approval approval = getApproval(id);
        approval.setEscalatedTo(escalateTo);
        approval.setStatus("Escalated");
    }

    private Approval getApproval(UUID id) {
        return approvalDatabase.computeIfAbsent(id, k -> new Approval(id));
    }
}

class Approval {
    private UUID id;
    private String status;
    private String reason;
    private UUID delegatedTo;
    private UUID escalatedTo;

    public Approval(UUID id) {
        this.id = id;
        this.status = "Pending";
    }

    public void setStatus(String status) { this.status = status; }
    public void setReason(String reason) { this.reason = reason; }
    public void setDelegatedTo(UUID delegatedTo) { this.delegatedTo = delegatedTo; }
    public void setEscalatedTo(UUID escalatedTo) { this.escalatedTo = escalatedTo; }
}

class RejectRequest {
    private String reason;
    public String getReason() { return reason; }
}

class DelegateRequest {
    private UUID delegateTo;
    public UUID getDelegateTo() { return delegateTo; }
}

class EscalateRequest {
    private UUID escalateTo;
    public UUID getEscalateTo() { return escalateTo; }
}
