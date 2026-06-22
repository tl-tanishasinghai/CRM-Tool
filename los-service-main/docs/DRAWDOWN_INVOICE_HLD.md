# High Level Design (HLD): Drawdown and Invoice Module

## 1. Overview

The Drawdown and Invoice module enables partners to request disbursements (drawdowns) against approved credit lines. It supports two product types:

- **PRODUCT_FUND**: Invoice-backed drawdowns — validates and persists invoices, then executes the drawdown.
- **PRODUCT_KCL**: Standalone drawdowns — executes drawdown without invoice validation.

---

## 2. System Context

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           LOS Service (Drawdown Module)                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                   │
│   ┌─────────────┐     ┌──────────────────┐     ┌─────────────────────────────┐   │
│   │   Partner   │────▶│ DrawdownController│────▶│   DrawdownOrchestrator     │   │
│   │   (Client)  │     │   (REST API)      │     │   (Flow Orchestration)       │   │
│   └─────────────┘     └──────────────────┘     └──────────────┬──────────────┘   │
│                                                               │                   │
│                                                               ▼                   │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                     DrawdownService / InvoiceService                     │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│                                      │                                           │
└──────────────────────────────────────┼───────────────────────────────────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         │                             │                             │
         ▼                             ▼                             ▼
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   M2P (Vendor)  │         │  Risk Service   │         │  Partner API    │
│   - Trigger     │         │  - BRE          │         │  - Callbacks    │
│   - Approve     │         │  - Funnel Rules │         │                 │
│   - Reject      │         │  - Dedupe       │         │                 │
└─────────────────┘         └─────────────────┘         └─────────────────┘
```

---

## 3. Key Components

| Component | Responsibility |
|-----------|----------------|
| **DrawdownController** | REST API entry point; accepts drawdown requests, approve/reject, status queries |
| **DrawdownOrchestrator** | Routes by product type; orchestrates invoice + drawdown or standalone drawdown flow |
| **DrawdownService** | Core business logic: validation, BRE, M2P trigger, status updates, callbacks |
| **InvoiceService** | Invoice validation, persistence, deduplication, drawdown-invoice mapping |
| **DrawdownUtil** | Shared utilities: validation, mapping, status response building |

---

## 4. API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/drawdown/{lineId}` | Process drawdown (create or idempotent return) |
| POST | `/api/v1/drawdown/approve/{transactionId}` | Approve OPS_APPROVAL_PENDING drawdown |
| POST | `/api/v1/drawdown/reject/{transactionId}` | Reject OPS_APPROVAL_PENDING drawdown |
| GET | `/api/v1/drawdown/{lineId}/status/{transactionId}` | Get status by M2P transaction ID |
| GET | `/api/v1/drawdown/{lineId}/external/{externalId}` | Get drawdown by external ID (idempotency key) |
| GET | `/api/v1/drawdown/transaction-details/{lineId}` | List all transaction details for a line |
| GET | `/api/v1/drawdown/transaction-details/active/{lineId}` | List active transaction details |

**Headers:** `PARTNER_ID`, `PRODUCT_CODE` (for POST drawdown)

---

## 5. Drawdown Flow (High Level)

### 5.1 Invoice + Drawdown Flow (PRODUCT_FUND)

```
Request → Idempotency Check (externalId)
       → Validate Invoices
       → Validate Anchor & GST
       → Validate & Persist Invoices
       → Persist Drawdown + Invoice Mappings
       → Funnel/Risk Dedupe Check
       → BRE (Business Rules Engine)
       → Upload Agreement (if present)
       → Trigger M2P Drawdown
       → Update Status to OPS_APPROVAL_PENDING
       → Return Response
```

### 5.2 Standalone Drawdown Flow (PRODUCT_KCL)

```
Request → Idempotency Check (externalId)
       → Validate Anchor
       → Persist Drawdown
       → Funnel/Risk Dedupe Check
       → BRE
       → Upload Agreement (if present)
       → Trigger M2P Drawdown
       → Update Status to OPS_APPROVAL_PENDING
       → Return Response
```

### 5.3 Approval / Rejection Flow (OPS)

```
Approve/Reject Request → M2P Approve/Reject API
                      → Update Drawdown Status
                      → Partner Callback
```

---

## 6. Drawdown Status Lifecycle

```
                    ┌─────────────┐
                    │    INIT      │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │  BRE_INIT    │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
     ┌──────────────┐ ┌─────────────┐ ┌─────────────┐
     │ BRE_APPROVED │ │BRE_REJECTED │ │RISK_REJECTED│
     └──────┬───────┘ └─────────────┘ └─────────────┘
            │         (Final)          (Final)
            ▼
     ┌─────────────────────┐
     │ OPS_APPROVAL_PENDING│
     └──────────┬──────────┘
                │
       ┌────────┼────────┐
       ▼        ▼         ▼
  ┌────────┐ ┌─────────┐ ┌────────┐
  │SUCCESS │ │OPS_REJ. │ │ FAILED │
  └────────┘ └─────────┘ └────────┘
  (Final)    (Final)     (Final)
```

**Final Statuses:** SUCCESS, FAILED, BRE_REJECTED, RISK_REJECTED, OPS_REJECTED

---

## 7. Invoice Module (High Level)

### 7.1 Purpose

- Store invoice data for invoice-backed drawdowns (PRODUCT_FUND)
- Deduplicate invoices via hash key
- Prevent reuse of invoices used in successful/pending drawdowns
- Link invoices to drawdowns via `drawdown_invoice_mappings`

### 7.2 Invoice Hash Key

```
hashKey = SHA256(partnerId | anchorId | invoiceNumber | invoiceDate | identityKey)
```

- Ensures uniqueness per invoice
- Supports idempotent invoice creation

### 7.3 Invoice–Drawdown Relationship

```
invoices (1) ──────< drawdown_invoice_mappings >────── (1) drawdowns
```

- One invoice can be used in multiple drawdowns only if it was previously FAILED
- One drawdown can reference multiple invoices

---

## 8. External Integrations

| System | Role |
|--------|------|
| **M2P** | Drawdown trigger, approve, reject; transaction status; callbacks |
| **Risk Service** | BRE eligibility; funnel rules (UHR, NPA, active loans); dedupe |
| **Partner API** | Callback delivery on approval/rejection |
| **Product Config** | Product-specific flow and BRE configuration |
| **Anchor Master** | Anchor validation, PAN lookup |

---

## 9. Key Design Decisions

| Decision | Rationale |
|----------|------------|
| **Idempotency via externalId** | Partners can retry safely; scoped by partnerId + lineId |
| **Product-based routing** | PRODUCT_FUND vs PRODUCT_KCL have different validation and persistence needs |
| **Invoice hash key** | Deterministic deduplication; same invoice data yields same hash |
| **Status timestamps** | `opsApprovalPendingAt`, `finalStatusAt` for reporting and analytics |
| **Dedicated exceptions** | `DrawdownBreRejectedException`, `DrawdownRiskRejectedException` for clear error handling and status preservation |

---

## 10. Data Model (Entity Level)

### Drawdown

- **id**, partnerId, anchorId, amount, status, transactionId, lineId, externalId
- **metadata** (JSONB): drawdown data (charges, EMI details, etc.)
- **opsApprovalPendingAt**, **finalStatusAt**

### Invoice

- **id**, partnerId, anchorId, invoiceNumber, amount, invoiceDate
- **metadata** (JSONB), **hashKey**

### DrawdownInvoiceMapping

- **drawdown_id**, **invoice_id** (many-to-many link)

### DrawdownAdditionalDetails

- **drawdownId**, loanAccountNumber, approvedAmount, netDisbursedAmount, disbursedDate, receiptNumber, rejectionReason
