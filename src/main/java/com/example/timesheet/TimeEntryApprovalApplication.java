package com.company.approvalsystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.web.bind.annotation.*;
import java.util.EnumSet;
import java.util.UUID;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final StateMachine<ApprovalState, ApprovalEvent> stateMachine;

    @Autowired
    public ApprovalController(StateMachine<ApprovalState, ApprovalEvent> stateMachine) {
        this.stateMachine = stateMachine;
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approve(@PathVariable UUID id) {
        sendEvent(ApprovalEvent.APPROVE);
        return ResponseEntity.ok("Time entry approved");
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<String> reject(@PathVariable UUID id, @RequestBody RejectRequest request) {
        sendEvent(ApprovalEvent.REJECT);
        return ResponseEntity.ok("Time entry rejected");
    }

    @PostMapping("/{id}/delegate")
    public ResponseEntity<String> delegateApproval(@PathVariable UUID id, @RequestBody DelegateRequest request) {
        sendEvent(ApprovalEvent.DELEGATE);
        return ResponseEntity.ok("Approval delegated");
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<String> escalateApproval(@PathVariable UUID id, @RequestBody EscalateRequest request) {
        sendEvent(ApprovalEvent.ESCALATE);
        return ResponseEntity.ok("Approval escalated");
    }

    private void sendEvent(ApprovalEvent event) {
        Message<ApprovalEvent> message = MessageBuilder.withPayload(event).build();
        stateMachine.sendEvent(message);
    }
}

@Configuration
@EnableStateMachine
class ApprovalStateMachine extends StateMachineConfigurerAdapter<ApprovalState, ApprovalEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<ApprovalState, ApprovalEvent> states) throws Exception {
        states
                .withStates()
                .initial(ApprovalState.PENDING)
                .states(EnumSet.of(ApprovalState.PENDING, ApprovalState.SUBMITTED, ApprovalState.APPROVED,
                        ApprovalState.REJECTED, ApprovalState.DELEGATED, ApprovalState.ESCALATED));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ApprovalState, ApprovalEvent> transitions) throws Exception {
        transitions
                .withExternal().source(ApprovalState.PENDING).target(ApprovalState.SUBMITTED).event(ApprovalEvent.SUBMIT)
                .and()
                .withExternal().source(ApprovalState.SUBMITTED).target(ApprovalState.APPROVED).event(ApprovalEvent.APPROVE)
                .and()
                .withExternal().source(ApprovalState.SUBMITTED).target(ApprovalState.REJECTED).event(ApprovalEvent.REJECT)
                .and()
                .withExternal().source(ApprovalState.SUBMITTED).target(ApprovalState.DELEGATED).event(ApprovalEvent.DELEGATE)
                .and()
                .withExternal().source(ApprovalState.SUBMITTED).target(ApprovalState.ESCALATED).event(ApprovalEvent.ESCALATE);
    }
}

enum ApprovalState {
    PENDING, SUBMITTED, APPROVED, REJECTED, DELEGATED, ESCALATED;
}

enum ApprovalEvent {
    SUBMIT, APPROVE, REJECT, DELEGATE, ESCALATE;
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
