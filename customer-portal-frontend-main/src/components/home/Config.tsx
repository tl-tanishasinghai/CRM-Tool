'use client'

export const HOME_TEXTS = {
  genericErrorMsg: "unexpected error occurred",
  welcome: "Welcome",
  tabs: {
    profile: "Profile",
    loans: "Loans",
  },
  profile: {
    customerId: "Customer ID",
    name: "Customer Name",
    age: "Age",
    dob: "Date Of Birth",
    address: "Permanent Address",
    number: "Registered Mobile Number",
  },
  loansTab: {
    filtersLabel: "Filter Loans",
    filters: {
        active: "Active",
        closed: "Closed",
        all: "All"
    },
    noLoans: "No Loans found",
    loanDetails: {
        row: {
            tenure: "Tenure",
            installmentAmt: "Installment Amount",
            disbursementDate: "Disbursement Date",
            interestRate: "ROI",
        },
        additionalDetails: {
            netDisbursement: "Net Disbursement Amount",
            lsp: "Lending Service Provider (LSP)",
            preDisbursement: "Pre Disbursement Charges",
            postDisburseMent: "Post Disbursement Charges",
            lastBureau: "Last Bureau reporting done on",
            lastPaymentAmt: "Last payment amount",
            lastPaymentDate: "Last payment date",
            nextPaymentDate: "Next payment date",
            nextPaymentAmt: "Next payment amount",
            principal: "Total principal outstanding"
        }
    }
  },
};
