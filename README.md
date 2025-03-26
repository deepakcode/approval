**Time Entry Approval Flow Design Document**

## **1. Introduction**

This document outlines the design of a scalable and efficient time entry approval system using Spring Boot microservices. The system allows time entries to be approved or rejected at a granular level, based on a selected time window. The approval flow now includes multi-level approval in case a manager is unavailable and allows delegation of approval to another approver.

## **2. Approval Flow**

### **2.1 Workflow**

1. **Explicit Submit Flow Enabled**

   - `Pending → Submit → Approve/Reject`
   - If rejected:
     - Worker can edit and resubmit.
     - Approver can edit and approve.
   - If the assigned approver is unavailable:
     - The next-level manager can approve/reject.
     - The approver can delegate approval to another authorized approver.

2. **Explicit Submit Flow Disabled**

   - `Pending → Approve/Reject`
   - No submission step required.
   - Multi-level approval and delegation rules apply as described above.

### **2.2 State Machine Diagram**

```text
          [Pending] --Submit--> [Submitted] --Approve--> [Approved]
             |                        |                      |
             |                        |                      |
             |                        -->Reject--> [Rejected] (Resubmit/Edit)
             -->Approve--> [Approved]
             |
             --> Delegation --> [New Approver] --Approve/Reject
             |
             --> Escalate to Manager --> [Next-Level Manager] --Approve/Reject
```

## **3. Architecture**

### **3.1 Microservices Involved**

1. **Time Entry Service** - Manages time entries.
2. **Approval Service** - Handles approval flow, state transitions, delegation, and escalation.
3. **User Service** - Manages employee, approver roles, and delegation settings.
4. **Notification Service** - Sends notifications upon state transitions.

### **3.2 Communication Mechanism**

- REST APIs for synchronous interactions.
- Kafka for asynchronous approval/rejection events.
- Redis for caching frequent queries.

## **4. Database Schema Design**

### **4.1 Tables**

#### **Time Entry Table**

| Column Name | Data Type | Description                 |
| ----------- | --------- | --------------------------- |
| id          | UUID      | Primary Key                 |
| worker\_id  | UUID      | Worker reference            |
| date        | DATE      | Entry date                  |
| clock\_in   | TIMESTAMP | Clock-in time               |
| clock\_out  | TIMESTAMP | Clock-out time              |
| status      | ENUM      | Pending, Approved, Rejected |

#### **Approval Table**

| Column Name     | Data Type | Description                       |
| --------------- | --------- | --------------------------------- |
| id              | UUID      | Primary Key                       |
| time\_entry\_id | UUID      | Foreign Key to Time Entry Table   |
| approver\_id    | UUID      | Approver reference                |
| status          | ENUM      | Approved, Rejected, Delegated     |
| reason          | TEXT      | Rejection reason (if any)         |
| delegated\_to   | UUID      | New approver (if delegated)       |
| escalated\_to   | UUID      | Next-level manager (if escalated) |
| timestamp       | TIMESTAMP | Approval/Rejection time           |

## **5. API Design**

### **5.1 Time Entry Service APIs**

- **Submit Time Entry:** `POST /time-entries/{id}/submit`
- **Get Time Entry:** `GET /time-entries/{id}`
- **List Worker Time Entries:** `GET /time-entries?workerId={workerId}`

### **5.2 Approval Service APIs**

- **Approve Time Entry:** `POST /approvals/{id}/approve`
- **Reject Time Entry:** `POST /approvals/{id}/reject`
- **Edit & Approve:** `PUT /approvals/{id}/edit`
- **Delegate Approval:** `POST /approvals/{id}/delegate`
  - **Request Body:**
    ```json
    {
      "delegateTo": "approver_uuid"
    }
    ```
- **Escalate Approval:** `POST /approvals/{id}/escalate`
  - **Request Body:**
    ```json
    {
      "escalateTo": "manager_uuid"
    }
    ```
- **Bulk Approve/Reject for Employees in Time Window:** `POST /approvals/bulk`
  - **Request Body:**
    ```json
    {
      "workerIds": ["uuid1", "uuid2"],
      "startDate": "YYYY-MM-DD",
      "endDate": "YYYY-MM-DD",
      "action": "approve/reject",
      "reason": "Optional rejection reason"
    }
    ```
  - **Description:** Approves or rejects time entries for a list of employees within a specified time window.

## **6. Scalability Considerations**

- **Batch Processing:** Bulk approval for multiple employees.
- **Caching Layer:** Redis caching for frequently accessed time entries.
- **Asynchronous Handling:** Kafka for event-driven state transitions.
- **Sharding Strategy:** Distribute time entries across database partitions.

## **7. Implementation Plan**

### **7.1 Phase 1: Core Services Implementation**

- Implement **Time Entry Service**: CRUD operations for time entries.
- Implement **Approval Service**: Approval/rejection workflow, delegation, and escalation.
- Implement **User Service**: User and role management.
- Implement **Notification Service**: Notify users of status changes.

### **7.2 Phase 2: Advanced Features**

- Implement **Kafka-based event handling** for approval status changes.
- Add **Redis caching** for quick retrieval of time entries.
- Optimize **batch processing** for bulk approvals.
- Implement **delegation and escalation rules** for multi-level approval.

### **7.3 Phase 3: UI and Final Enhancements**

- Develop **Front-end UI** for time entry submission and approval.
- Add **GraphQL support** for flexible data queries.
- Conduct **performance tuning and stress testing**.

---

The next step will be implementing the detailed Java code for this system.

