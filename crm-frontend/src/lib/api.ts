import axios from 'axios'
import type {
  AgentNote,
  ApiResponse,
  CallEvent,
  CrmLead,
  CustomerDashboard,
  CustomerProfileResponse,
  CustomerSummaryResponse,
  FreshdeskAgentTicket,
  FreshdeskTicketBucketResponse,
  FreshdeskTicketFilters,
  IntegrationsHealthResponse,
  IvrOverviewFilters,
  IvrOverviewResponse,
  LoanDetailResponse,
  LoanMiniSummary,
  LoanRepaymentSchedule,
  LoanTransactionHistory,
  OpsOverviewResponse,
  OpsTicketFilters,
  OpsTicketsPage,
  PiiField,
  SearchResult,
  StaffUser,
  TeamQueueResponse
} from './types'

const baseURL = process.env.NEXT_PUBLIC_CRM_API_BASE_URL || 'http://localhost:8092'

const api = axios.create({ baseURL })

api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('crmToken')
    if (token) {
      config.headers['X-CRM-Token'] = token
    }
  }
  return config
})

export async function login(email: string, password: string) {
  const response = await api.post<{ token: string; user: StaffUser }>('/crm/auth/login', {
    email,
    password
  })
  return response.data
}

export async function logout() {
  await api.post('/crm/auth/logout')
}

export async function fetchMe() {
  const response = await api.get<StaffUser>('/crm/auth/me')
  return response.data
}

export async function fetchMyQueue() {
  const response = await api.get<CrmLead[]>('/crm/leads/my-queue')
  return response.data
}

export async function fetchTeamQueue() {
  const response = await api.get<TeamQueueResponse>('/crm/leads/team-queue')
  return response.data
}

export async function fetchUsers() {
  const response = await api.get<StaffUser[]>('/crm/admin/users')
  return response.data
}

export async function createUser(payload: {
  name: string
  email: string
  role: string
  leadId?: string
  status: string
}) {
  const response = await api.post<StaffUser>('/crm/admin/users', payload)
  return response.data
}

export async function ingestLead(payload: {
  leadId: string
  clientId?: string
  mobileNumber?: string
  title?: string
  loanAccountNumber?: string
  loanApplicationId?: string
  source?: string
  priority?: string
}) {
  const response = await api.post<CrmLead>('/crm/leads/ingest', payload)
  return response.data
}

export async function assignLead(crmLeadId: string, agentId: string) {
  const response = await api.post<CrmLead>(`/crm/leads/${crmLeadId}/assign`, {
    agentId,
    reason: 'Manual CRM assignment'
  })
  return response.data
}

export async function updateLeadStatus(crmLeadId: string, status: string) {
  const response = await api.patch<CrmLead>(`/crm/leads/${crmLeadId}/status`, { status })
  return response.data
}

export async function searchCustomers(query: string) {
  const response = await api.get<SearchResult[]>('/crm/search', { params: { query } })
  return response.data
}

export async function fetchDashboard(leadId: string) {
  const response = await api.get<CustomerDashboard>(`/crm/customers/${leadId}/dashboard`)
  return response.data
}

export async function fetchCustomerProfile(leadId: string) {
  const response = await api.get<ApiResponse<CustomerProfileResponse>>(
    `/crm/customers/${leadId}/profile`
  )
  if (response.data.status !== 'SUCCESS' || !response.data.data) {
    throw new Error(response.data.errorMessage || 'Unable to load customer profile')
  }
  return response.data.data
}

export async function revealCustomerField(
  leadId: string,
  field: PiiField,
  reason?: string
) {
  const response = await api.post<ApiResponse<{ field: PiiField; value: string; auditId: string; revealedAt: string }>>(
    `/crm/customers/${leadId}/reveal`,
    { field, reason }
  )
  if (response.data.status !== 'SUCCESS' || !response.data.data) {
    throw new Error(response.data.errorMessage || 'Unable to reveal field')
  }
  return response.data.data
}

export async function fetchLoanDetail(leadId: string, loanAccountNumber: string) {
  const response = await api.get<ApiResponse<LoanDetailResponse>>(
    `/crm/customers/${leadId}/loans/${encodeURIComponent(loanAccountNumber)}/details`
  )
  if (response.data.status !== 'SUCCESS' || !response.data.data) {
    throw new Error(response.data.errorMessage || 'Unable to load loan details')
  }
  return response.data.data
}

export async function fetchLoanRps(leadId: string, loanAccountNumber: string) {
  const response = await api.get<LoanRepaymentSchedule>(
    `/crm/customers/${leadId}/loans/${encodeURIComponent(loanAccountNumber)}/rps`
  )
  return response.data
}

export async function fetchLoanTransactions(leadId: string, loanAccountNumber: string) {
  const response = await api.get<LoanTransactionHistory>(
    `/crm/customers/${leadId}/loans/${encodeURIComponent(loanAccountNumber)}/transactions`
  )
  return response.data
}

export async function fetchFreshdeskBucket(filters: FreshdeskTicketFilters = {}) {
  const response = await api.get<FreshdeskTicketBucketResponse>('/crm/freshdesk/tickets/my-bucket', {
    params: filters
  })
  return response.data
}

export async function syncFreshdeskTickets() {
  const response = await api.post<FreshdeskTicketBucketResponse>('/crm/freshdesk/tickets/sync')
  return response.data
}

export async function replyFreshdeskTicket(ticketId: string, body: string) {
  const response = await api.post<FreshdeskAgentTicket>(
    `/crm/freshdesk/tickets/${encodeURIComponent(ticketId)}/reply`,
    { body }
  )
  return response.data
}

export async function updateFreshdeskTicket(
  ticketId: string,
  payload: { status?: string; priority?: string }
) {
  const response = await api.patch<FreshdeskAgentTicket>(
    `/crm/freshdesk/tickets/${encodeURIComponent(ticketId)}`,
    payload
  )
  return response.data
}

export async function createTicket(leadId: string, subject: string, description: string) {
  const response = await api.post(`/crm/customers/${leadId}/tickets`, {
    subject,
    description,
    category: 'Loan servicing',
    priority: 'MEDIUM'
  })
  return response.data
}

export async function createNote(leadId: string, note: string, disposition: string) {
  const response = await api.post<AgentNote>(`/crm/customers/${leadId}/notes`, {
    note,
    disposition
  })
  return response.data
}

export async function createCallEvent(leadId: string, phoneNumber: string, disposition: string) {
  const response = await api.post<CallEvent>('/crm/calls/events', {
    leadId,
    phoneNumber,
    disposition,
    direction: 'OUTBOUND',
    durationSeconds: 0
  })
  return response.data
}

function opsRangeParams(days = 7) {
  const to = new Date()
  const from = new Date(to.getTime() - days * 24 * 60 * 60 * 1000)
  return {
    from: from.toISOString(),
    to: to.toISOString()
  }
}

export async function fetchOpsOverview(days = 7) {
  const response = await api.get<OpsOverviewResponse>('/crm/admin/ops/overview', {
    params: opsRangeParams(days)
  })
  return response.data
}

export async function fetchOpsTickets(filters: OpsTicketFilters = {}) {
  const response = await api.get<OpsTicketsPage>('/crm/admin/ops/tickets', {
    params: filters
  })
  return response.data
}

export async function fetchFreshdeskOrgBucket(filters: FreshdeskTicketFilters = {}) {
  const response = await api.get<FreshdeskTicketBucketResponse>(
    '/crm/freshdesk/tickets/org-bucket',
    { params: filters }
  )
  return response.data
}

export async function fetchFreshdeskTeamBucket(
  leadId?: string,
  filters: FreshdeskTicketFilters = {}
) {
  const response = await api.get<FreshdeskTicketBucketResponse>(
    '/crm/freshdesk/tickets/team-bucket',
    { params: { ...filters, leadId } }
  )
  return response.data
}

export async function fetchIntegrationsHealth() {
  const response = await api.get<IntegrationsHealthResponse>('/crm/admin/ops/health')
  return response.data
}

export async function fetchCustomerSummary(leadId: string) {
  const response = await api.get<ApiResponse<CustomerSummaryResponse>>(
    `/crm/customers/${leadId}/summary`
  )
  if (response.data.status !== 'SUCCESS' || !response.data.data) {
    throw new Error(response.data.errorMessage || 'Unable to load customer summary')
  }
  return response.data.data
}

export async function fetchIvrOverview(filters: IvrOverviewFilters = {}) {
  const response = await api.get<IvrOverviewResponse>('/crm/ivr/overview', { params: filters })
  return response.data
}
