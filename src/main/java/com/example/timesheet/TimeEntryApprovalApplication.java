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

// Entity for Time Entry
package com.example.timesheet.entity;

        import jakarta.persistence.*;
        import java.time.LocalDateTime;
        import java.util.UUID;

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
    private String status; // PENDING, APPROVED, REJECTED

    // Getters and Setters
}

// Entity for Approval
package com.example.timesheet.entity;

        import jakarta.persistence.*;
        import java.time.LocalDateTime;
        import java.util.UUID;

@Entity
@Table(name = "approvals")
public class Approval {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private UUID timeEntryId;
    private UUID approverId;
    private String status; // APPROVED, REJECTED
    private String reason;
    private LocalDateTime timestamp;

    // Getters and Setters
}

// Repository for Time Entry
package com.example.timesheet.repository;

        import com.example.timesheet.entity.TimeEntry;
        import org.springframework.data.jpa.repository.JpaRepository;
        import java.time.LocalDateTime;
        import java.util.List;
        import java.util.UUID;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {
    List<TimeEntry> findByWorkerIdInAndDateBetween(List<UUID> workerIds, LocalDateTime startDate, LocalDateTime endDate);
}

// Repository for Approval
package com.example.timesheet.repository;

        import com.example.timesheet.entity.Approval;
        import org.springframework.data.jpa.repository.JpaRepository;
        import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {}

// Service for Approval
package com.example.timesheet.service;

        import com.example.timesheet.entity.Approval;
        import com.example.timesheet.entity.TimeEntry;
        import com.example.timesheet.repository.ApprovalRepository;
        import com.example.timesheet.repository.TimeEntryRepository;
        import org.springframework.stereotype.Service;
        import org.springframework.transaction.annotation.Transactional;
        import java.time.LocalDateTime;
        import java.util.List;
        import java.util.UUID;

@Service
public class ApprovalService {
    private final ApprovalRepository approvalRepository;
    private final TimeEntryRepository timeEntryRepository;

    public ApprovalService(ApprovalRepository approvalRepository, TimeEntryRepository timeEntryRepository) {
        this.approvalRepository = approvalRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Transactional
    public void approveEntries(List<UUID> workerIds, LocalDateTime startDate, LocalDateTime endDate) {
        List<TimeEntry> entries = timeEntryRepository.findByWorkerIdInAndDateBetween(workerIds, startDate, endDate);
        entries.forEach(entry -> entry.setStatus("APPROVED"));
        timeEntryRepository.saveAll(entries);

        List<Approval> approvals = entries.stream().map(entry -> {
            Approval approval = new Approval();
            approval.setTimeEntryId(entry.getId());
            approval.setStatus("APPROVED");
            approval.setTimestamp(LocalDateTime.now());
            return approval;
        }).toList();

        approvalRepository.saveAll(approvals);
    }
}

// Controller for Time Entry and Approval
package com.example.timesheet.controller;

        import com.example.timesheet.service.ApprovalService;
        import org.springframework.web.bind.annotation.*;
        import java.time.LocalDateTime;
        import java.util.List;
        import java.util.UUID;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {
    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/bulk")
    public void approveBulkEntries(@RequestBody BulkApprovalRequest request) {
        approvalService.approveEntries(request.getWorkerIds(), request.getStartDate(), request.getEndDate());
    }
}

// DTO for Bulk Approval
package com.example.timesheet.dto;

        import java.time.LocalDateTime;
        import java.util.List;
        import java.util.UUID;

public class BulkApprovalRequest {
    private List<UUID> workerIds;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Getters and Setters
}
