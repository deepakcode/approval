package com.example.timesheet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableJpaRepositories
@EnableFeignClients
@ComponentScan(basePackages = "com.example.timesheet")
public class TimeEntryApprovalApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimeEntryApprovalApplication.class, args);
    }
}

// Enum for Approval States
package com.example.timesheet.state;

public enum ApprovalState {
    PENDING, MANAGER_APPROVED, HR_APPROVED, FINANCE_APPROVED, REJECTED
}

// Entity for Time Entry with State Machine Support
package com.example.timesheet.entity;

        import jakarta.persistence.*;
        import java.time.LocalDateTime;
        import java.util.UUID;
        import com.example.timesheet.state.ApprovalState;

@Entity
@Table(name = "time_entries")
public class TimeEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private UUID workerId;
    private LocalDateTime date;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;

    @Enumerated(EnumType.STRING)
    private ApprovalState status; // Tracks state transitions

    // Getters and Setters
}

// State Machine Configuration
package com.example.timesheet.config;

        import com.example.timesheet.state.ApprovalState;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.statemachine.config.EnableStateMachine;
        import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
        import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
        import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
        import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

        import java.util.EnumSet;

@Configuration
@EnableStateMachine
public class ApprovalStateMachineConfig extends StateMachineConfigurerAdapter<ApprovalState, String> {

    @Override
    public void configure(StateMachineStateConfigurer<ApprovalState, String> states) throws Exception {
        states
                .withStates()
                .initial(ApprovalState.PENDING)
                .states(EnumSet.allOf(ApprovalState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ApprovalState, String> transitions) throws Exception {
        transitions
                .withExternal().source(ApprovalState.PENDING).target(ApprovalState.MANAGER_APPROVED).event("MANAGER_APPROVE")
                .and()
                .withExternal().source(ApprovalState.MANAGER_APPROVED).target(ApprovalState.HR_APPROVED).event("HR_APPROVE")
                .and()
                .withExternal().source(ApprovalState.HR_APPROVED).target(ApprovalState.FINANCE_APPROVED).event("FINANCE_APPROVE")
                .and()
                .withExternal().source(ApprovalState.PENDING).target(ApprovalState.REJECTED).event("REJECT");
    }
}

// Service for Approval with State Transitions
package com.example.timesheet.service;

        import com.example.timesheet.entity.TimeEntry;
        import com.example.timesheet.repository.TimeEntryRepository;
        import com.example.timesheet.state.ApprovalState;
        import org.springframework.statemachine.StateMachine;
        import org.springframework.stereotype.Service;
        import org.springframework.transaction.annotation.Transactional;

        import java.util.UUID;

@Service
public class ApprovalService {
    private final TimeEntryRepository timeEntryRepository;
    private final StateMachine<ApprovalState, String> stateMachine;

    public ApprovalService(TimeEntryRepository timeEntryRepository, StateMachine<ApprovalState, String> stateMachine) {
        this.timeEntryRepository = timeEntryRepository;
        this.stateMachine = stateMachine;
    }

    @Transactional
    public void approve(UUID timeEntryId, String approverRole) {
        TimeEntry entry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new RuntimeException("Time entry not found"));

        String event = switch (approverRole) {
            case "MANAGER" -> "MANAGER_APPROVE";
            case "HR" -> "HR_APPROVE";
            case "FINANCE" -> "FINANCE_APPROVE";
            default -> throw new IllegalArgumentException("Invalid approver role");
        };

        stateMachine.sendEvent(event);
        entry.setStatus(stateMachine.getState().getId());
        timeEntryRepository.save(entry);
    }
}

// Controller for Approvals
package com.example.timesheet.controller;

        import com.example.timesheet.service.ApprovalService;
        import org.springframework.web.bind.annotation.*;
        import java.util.UUID;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {
    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/{timeEntryId}/{role}")
    public void approveEntry(@PathVariable UUID timeEntryId, @PathVariable String role) {
        approvalService.approve(timeEntryId, role);
    }
}
