import React,{useEffect} from "react";
import "./LoanDetails.scss";
import { useRouter } from 'next/navigation';
import paymentPending from "../../assets/images/payment-failure-pending.png"
import paymentSuccess from "../../assets/images/paymentSuccess.png"
import paymentFailed from "../../assets/images/paymentFailure.png"
import LoanActive from "../../assets/images/LoanActive.svg";
import LoanClosed from "../../assets/images/LoanClosed.svg";
// import formatWithCommas from "../../utils/formatCommas";


interface LoanGridItem {
  type: string;
  label?: string;
  value?: string;
  className?: string;
}

interface LoanRowItem {
  label: string;
  value: string;
}

interface AdditionalDetailItem {
  label: string;
  value: string;
}

interface LoanDetailsProps {
  loanRowDetails: LoanRowItem[];
  loanGridDetails: LoanGridItem[];
  additionalDetails: AdditionalDetailItem[];
  leadID: string;
  LAN: string;
  collectionDetails?: any;
  latestCollectionDetails?: any;
  currentDueSplit?: any;
  foreclosureSplit?: any;
  nextDueSplit?: any;
  nextPaymentDue?: any;
}

const LoanDetails = (props: LoanDetailsProps) => {
  // const { loanInfo } = props;
  const {loanRowDetails, loanGridDetails, additionalDetails,leadID, LAN, collectionDetails,latestCollectionDetails,currentDueSplit, foreclosureSplit, nextDueSplit, nextPaymentDue} = props;
  const router = useRouter();

  const LOANS_STATUSES = {
    ACTIVE: 'Active',
    CLOSED: 'Closed',
    WRITTEN_OFF: 'Written off',
    OVERPAID: 'Overpaid'
  } as const;

  // useEffect(() => {
  //   return () => localStorage.removeItem("collectionDetails");
  // }, []);

  const getStatusLabel= (labelEnum: string): string => {
    if(labelEnum in LOANS_STATUSES){
      return LOANS_STATUSES[labelEnum as keyof typeof LOANS_STATUSES]
    }
    return labelEnum
  }

  const handleRepayClick = () => {
    if (collectionDetails) {
      localStorage.setItem("collectionDetails", JSON.stringify(collectionDetails));
    }
    if (nextPaymentDue) {
      localStorage.setItem(
        "nextPaymentDue",
        JSON.stringify(nextPaymentDue)
      );
    }
    // STORE SPLITS FOR REPAYLOANS COMPONENT
    localStorage.setItem("repay_splits",
      JSON.stringify({
        currentDueSplit,
        foreclosureSplit,
        nextDueSplit
      })
    );
    const loanAccountNumber = LAN;
    // remove leading zeros
    const numericLAN = Number(loanAccountNumber?.replace(/^0+/, ""));

    router.push(`/repayLoans?leadID=${leadID}&loanAccount=${numericLAN}`);
  };

  const getLatestTransactionStatus = () => {
    const paymentStatus = latestCollectionDetails?.paymentStatus;
    const collectionStatus = latestCollectionDetails?.collectionStatus;
  
    if (!paymentStatus) return null;
  
    // Case 3: do nothing
    if (paymentStatus === "INITIATED") return null;
  
    // Case 2: force failed
    if (["CANCELLED", "EXPIRED", "FAILED"].includes(paymentStatus)) {
      return "FAILED";
    }
  
    // Case 1: payment PAID → trust collectionStatus
    if (paymentStatus === "PAID") {
      return collectionStatus;
    }
  
    return null;
  };
  
  const latestTransactionStatus = getLatestTransactionStatus();
  

  return (
    <div className="loan-details">
      <div className="header">
        <span>Loan Details</span>
      </div>
      {latestCollectionDetails && latestTransactionStatus && (
        <div
          className={`repay-loan-transaction-highlight ${latestTransactionStatus?.toLowerCase()}`}
        >
            <img
              src={
                latestTransactionStatus === "PAID"
                  ? paymentSuccess.src
                  : latestTransactionStatus === "FAILED"
                  ? paymentFailed.src
                  : paymentPending.src
              }
              alt="payment status"
              className="transaction-status-icon"
            />
          <span>
            {latestTransactionStatus === "PAID" &&
              `Payment of ₹${latestCollectionDetails?.amount ?? 0} has been successfully received and settled against your Loan account no. ${LAN ?? 0}`}
            { latestTransactionStatus === "FAILED" &&
              `Payment of ₹${latestCollectionDetails?.amount ?? 0} has failed. Please try again later.`}
            { latestTransactionStatus === "INITIATED" &&
              `Payment of ₹${latestCollectionDetails?.amount ?? 0} is being processed, loan details will update in sometime.`}
          </span>
        </div>
      )}
      <div className="details-card">
      <div className="details-card-2">
      <div className="top-section">
        <div className="logo-amount-container">
          {loanGridDetails.map((item, index) => {
            // if (item.type === "image") {
            //   return (
            //     <div className="loan-details-logo-container" key={index}>
            //       <img
            //         src={item.src}
            //         alt={item.alt}
            //         className={item.className}
            //       />
            //     </div>
            //   );
            // }

            if (item.type === "label-value") {
              return (
                <div className="amount-container" key={index}>
                  <div className="loan-label">{item.label}</div>
                  <div className={item.className}>{item.value}</div>
                </div>
              );
            }

            return null;
          })}
        </div>

        {loanGridDetails.map((item: LoanGridItem, index: number) => {
          if (item.type === "status" && item.value) {
            const statuslabel = getStatusLabel(item.value)
            return (
              <div className={item.className} key={index}>
                <img
                  src={statuslabel.toLowerCase() === "active" ? LoanActive.src : LoanClosed.src}
                  alt={statuslabel}
                />
                {statuslabel}
              </div>
            );
          }
          return null;
        })}
      </div>
      </div>

        <div className="divider"></div>
        
        <div className="main-details">
          <div className="row">
            {loanRowDetails.map((item, idx) => (
              <div className="tenure-block" key={idx}>
                <span className="value">{item.value}</span>
                <span className="label">{item.label}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="divider"></div>
        <div className="additional-details">
          {additionalDetails.map((item, idx) => (
            <div className="label-value-container" key={idx}>
              <span className="value">{item.value}</span>
              <span className="label">{item.label}</span>
            </div>
          ))}
        </div>

        {/* PG integration starts */}
        <div className="divider"></div>
        <button 
        className={`repay-button
          ${
            !collectionDetails || collectionDetails.isDirectPaymentEnabled === false || collectionDetails.isDirectPaymentEnabled === null
              ? "disabled"
              : ""
          }
        `}
        onClick={handleRepayClick}>
        Repay Loan</button>       
      </div>
    </div>
  );
};

export default LoanDetails;
