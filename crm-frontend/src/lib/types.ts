export type Role = 'ADMIN' | 'LEAD' | 'AGENT'
export type UserStatus = 'ACTIVE' | 'INACTIVE'
export type LeadStatus = 'NEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'FOLLOW_UP' | 'ESCALATED' | 'CLOSED'
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type StaffUser = {
  id: string
  name: string
  email: string
  role: Role
  status: UserStatus
  leadId?: string | null
  createdAt: string
}

export type CrmLead = {
  id: string
  leadId: string
  clientId?: string | null
  mobileNumber?: string | null
  title?: string | null
  loanAccountNumber?: string | null
  loanApplicationId?: string | null
  status: LeadStatus
  priority: Priority
  source: string
  assignedAgentId?: string | null
  assignedLeadId?: string | null
  assignedAt?: string | null
  createdAt: string
  updatedAt: string
}

export type ApiStatus = 'SUCCESS' | 'FAILURE'

export type ApiResponse<T> = {
  status: ApiStatus
  data: T | null
  errorCode?: string | null
  errorMessage?: string | null
}

export type PiiField = 'MOBILE' | 'EMAIL' | 'ADDRESS'

export type MaskedIdentity = {
  name: string
  mobileMasked: string
  emailMasked: string
  addressMasked: string
  addressShort: string
  panLast4: string
  dateOfBirth?: string | null
  leadId: string
  ucic: string
  clientId: string
  dataSource: string
}

export type LoanMiniSummary = {
  loanAccountNumber: string
  loanApplicationId?: string | null
  product: string
  status: string
  lenderName: string
  sanctionedAmount: number
  disbursedAmount: number
  netDisbursedAmount: number
  processingFee: number
  tenureMonths?: number | null
  roiPercent: number
  disbursementDate?: string | null
  dpdDays?: number | null
  outstanding: number
  emi: number
}

export type CustomerProfileResponse = {
  identity: MaskedIdentity
  loanAccounts: LoanMiniSummary[]
  dataSource: string
}

export type CustomerSummaryResponse = {
  customerFound: boolean
  leadId: string
  identity: MaskedIdentity
  activeLoans: LoanMiniSummary[]
  closedLoans: LoanMiniSummary[]
  loanAccounts: LoanMiniSummary[]
  dataSource: string
}

export type AmountBreakup = {
  principal: number
  interest: number
  charges: number
  penalties: number
}

export type LoanDetailResponse = {
  loanAccountNumber: string
  productName: string
  lenderName: string
  status: string
  normalizedStatus: string
  dpdDays?: number | null
  summary: {
    sanctionedAmount: number
    disbursedAmount: number
    netDisbursedAmount: number
    processingFeeAndCharges: number
    tenureMonths?: number | null
    roiPercent: number
    disbursementDate?: string | null
  }
  currentPosition: {
    principalOutstanding: number
    totalOutstanding: number
    nextInstallmentDate?: string | null
    nextInstallmentAmount: number
    nextInstallmentBreakup: AmountBreakup
    overdueAmount: number
    overdueBreakup: AmountBreakup
    dpdDays?: number | null
  }
  lifetimeTotals: {
    principalPaid: number
    interestPaid: number
    chargesPaid: number
    penaltiesPaid: number
    totalPaid: number
  }
  recentPayments: Array<{
    date: string
    amount: number
    type: string
    status: string
  }>
  foreclosureQuote: {
    totalDue?: number | null
    breakup?: AmountBreakup | null
    validUntil?: string | null
    available: boolean
  }
  applicationStatus: {
    loanApplicationId?: string | null
    receivedDate?: string | null
    lspName?: string | null
    stage?: string | null
  }
  documentsAvailable: string[]
  repaymentSchedule: LoanRepaymentSchedule
  transactions: LoanTransactionHistory
  dataSource: string
}

export type CustomerProfile = {
  leadId: string
  clientId: string
  name: string
  mobileNo: string
  email: string
  panLast4: string
  dateOfBirth: string
  city: string
  address: string
  ucic: string
  dataSource: string
}

export type LoanSummary = {
  loanAccountNumber: string
  loanApplicationId: string
  product: string
  status: string
  lenderName: string
  currentStage: string
  applicationStatus: string
  principal: number
  outstanding: number
  emi: number
  paidAmount: number
  remainingAmount: number
  excessAdjusted: number
  excessRefunded: number
  interestRate: number
  dpd: number
  tenureMonths: number
  installmentRemaining: number
  disbursementDate: string
  nextDueDate?: string | null
  message: string
}

export type RepaymentScheduleRow = {
  period?: number | null
  days?: number | null
  date: string
  paidDate?: string | null
  emiPaidDate?: string | null
  principal: number
  principalOutstanding: number
  interest: number
  fees: number
  penalties: number
  due: number
  paid: number
  outstanding: number
  inAdvance: number
  late: number
}

export type LoanRepaymentSchedule = {
  loanAccountNumber: string
  scheduleType: string
  periods: RepaymentScheduleRow[]
}

export type LoanTransactionRow = {
  office: string
  transactionDate: string
  transactionType: string
  amount: number
  principal: number
  interest: number
  fees: number
  penalties: number
  principalOutstanding: number
  transactionStatus: string
  excessAmount: number
}

export type LoanTransactionHistory = {
  loanAccountNumber: string
  transactions: LoanTransactionRow[]
}

export type TicketSummary = {
  id: string
  leadId: string
  subject: string
  status: string
  priority: string
  category: string
  createdAt: string
  updatedAt: string
}

export type FreshdeskConversationEntry = {
  id: string
  author: string
  body: string
  createdAt: string
  agentReply: boolean
}

export type FreshdeskAgentTicket = {
  id: string
  freshdeskId: string
  leadId: string
  subject: string
  requesterName: string
  requesterEmail: string
  mobileNumber: string
  loanAccountNumber: string
  status: 'OPEN' | 'PENDING' | 'RESOLVED' | 'CLOSED'
  priority: Priority
  category: string
  assigneeName: string
  assigneeEmail: string
  channel: string
  slaHint: string
  createdAt: string
  updatedAt: string
  closedAt?: string | null
  conversations: FreshdeskConversationEntry[]
}

export type FreshdeskBucketSummary = {
  total: number
  openCount: number
  pendingCount: number
  resolvedCount: number
  closedCount: number
}

export type FreshdeskTicketBucketResponse = {
  tickets: FreshdeskAgentTicket[]
  summary: FreshdeskBucketSummary
  syncedAt: string
  freshdeskConfigured: boolean
}

export type FreshdeskTicketFilters = {
  query?: string
  priority?: string
  mobileNumber?: string
  loanAccountNumber?: string
  createdFrom?: string
  createdTo?: string
  closedFrom?: string
  closedTo?: string
}

export type CallEvent = {
  id: string
  leadId: string
  agentId: string
  direction: string
  disposition: string
  phoneNumber: string
  durationSeconds?: number | null
  recordingUrl?: string | null
  startedAt: string
  createdAt: string
}

export type AgentNote = {
  id: string
  leadId: string
  agentId: string
  disposition?: string | null
  note: string
  followUpAt?: string | null
  createdAt: string
}

export type ActivityItem = {
  id: string
  type: string
  title: string
  description: string
  actorId?: string | null
  occurredAt: string
}

export type CustomerDashboard = {
  profile: CustomerProfile
  loans: LoanSummary[]
  tickets: TicketSummary[]
  calls: CallEvent[]
  notes: AgentNote[]
  activity: ActivityItem[]
}

export type SearchResult = {
  type: string
  leadId: string
  clientId?: string | null
  mobileNumber?: string | null
  loanAccountNumber?: string | null
  loanApplicationId?: string | null
  displayName: string
  matchedOn: string
}

export type TeamQueueResponse = {
  agents: StaffUser[]
  leads: CrmLead[]
  summary: {
    total: number
    newItems: number
    followUps: number
    escalated: number
  }
  crmBuckets: CrmBucketSummary
}

export type CrmBucketSummary = {
  total: number
  newCount: number
  assignedCount: number
  inProgressCount: number
  followUpCount: number
  escalatedCount: number
  closedCount: number
}

export type OpsRateMetrics = {
  crmResolutionRate: number
  freshdeskResolutionRate: number
  escalationRate: number
  crmOpenBacklog: number
  freshdeskOpenBacklog: number
  avgCrmCloseHours?: number | null
  avgFreshdeskCloseHours?: number | null
}

export type TeamOpsRow = {
  leadId: string
  leadName: string
  agentCount: number
  openCrm: number
  openFreshdesk: number
  crmResolutionRate: number
  freshdeskResolutionRate: number
  escalations: number
}

export type AgentOpsRow = {
  agentId: string
  agentName: string
  teamLeadId?: string | null
  teamLeadName?: string | null
  openCrm: number
  openFreshdesk: number
  resolvedCrm7d: number
  resolvedFreshdesk7d: number
  avgHandleHours?: number | null
}

export type OpsOverviewResponse = {
  from: string
  to: string
  crmBuckets: CrmBucketSummary
  freshdeskBuckets: FreshdeskBucketSummary
  rates: OpsRateMetrics
  teams: TeamOpsRow[]
  agents: AgentOpsRow[]
}

export type OpsTicketSource = 'CRM' | 'FRESHDESK'

export type OpsTicketRow = {
  id: string
  source: OpsTicketSource
  leadId: string
  title: string
  status: string
  priority: string
  assigneeId?: string | null
  assigneeName?: string | null
  teamLeadId?: string | null
  teamLeadName?: string | null
  mobileNumber?: string | null
  loanAccountNumber?: string | null
  createdAt: string
  updatedAt: string
  closedAt?: string | null
}

export type OpsTicketsPage = {
  tickets: OpsTicketRow[]
  total: number
  page: number
  size: number
}

export type OpsTicketFilters = {
  source?: string
  status?: string
  leadId?: string
  agentId?: string
  mobileNumber?: string
  loanAccountNumber?: string
  from?: string
  to?: string
  page?: number
  size?: number
}

export type IntegrationsHealthResponse = {
  freshdeskConfigured: boolean
  freshdeskLastSync: string
  exotelConfigured: boolean
  exotelStatus: string
  losLmsLiveData: boolean
}

export type IvrCallSource =
  | 'GREYLABS_BOT'
  | 'EXOTEL_INBOUND'
  | 'EXOTEL_OUTBOUND'
  | 'AGENT_MANUAL'

export type IvrCallRow = {
  callId: string
  callSid: string
  agentId?: string | null
  agentName: string
  leadId?: string | null
  clientId?: string | null
  mobileNumber?: string | null
  loanAccountNumber?: string | null
  callSummary: string
  freshdeskTicketId?: string | null
  freshdeskTicketRef?: string | null
  ticketStatus?: string | null
  direction: string
  disposition: string
  callSource: IvrCallSource
  categoryL1?: string | null
  categoryL2?: string | null
  categoryL3?: string | null
  durationSeconds?: number | null
  recordingUrl?: string | null
  startedAt: string
  syncStatus: string
}

export type IvrOverviewSummary = {
  total: number
  greylabsBot: number
  escalatedToTicket: number
  connected: number
  callbackRequested: number
}

export type IvrOverviewResponse = {
  calls: IvrCallRow[]
  total: number
  page: number
  size: number
  summary: IvrOverviewSummary
}

export type IvrOverviewFilters = {
  query?: string
  leadId?: string
  mobileNumber?: string
  loanAccountNumber?: string
  disposition?: string
  callSource?: string
  from?: string
  to?: string
  page?: number
  size?: number
}
