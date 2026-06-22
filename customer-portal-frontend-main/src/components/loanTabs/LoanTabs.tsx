// import React, { useRef, useState } from "react";
// import "./LoanTabs.scss";
// import rightArrowBlue from "../../assets/images/Arrow-Right-lightBlue.svg"
// const LoanTabs = (props) => {
//   const { loanList, selectedLoan, handleTabClick } = props;
//   const tabs = Array.from({ length: 10 }, (_, index) => ({
//     logo: "logo.png", // Replace with actual logo paths
//     company: "company name",
//     amount: "₹12,00,000",
//     date: "12/04/2024",
//     status: "Active",
//   }));

//   const containerRef = useRef(null);
//   const [isLeftArrowVisible, setIsLeftArrowVisible] = useState(false);
//   const [isRightArrowVisible, setIsRightArrowVisible] = useState(true);

//   const handleScroll = () => {
//     const container = containerRef.current;
//     if (!container) return;

//     setIsLeftArrowVisible(container.scrollLeft > 0);
//     setIsRightArrowVisible(
//       container.scrollLeft + container.offsetWidth < container.scrollWidth
//     );
//   };

//   const LOANS_STATUSES = {
//     ACTIVE: 'Active',
//     CLOSED: 'Closed',
//     WRITTEN_OFF: 'Written off',
//     OVERPAID: 'Overpaid'
//   }

//   const getStatusLabel= (labelEnum) => {
//     if(LOANS_STATUSES[labelEnum])return LOANS_STATUSES[labelEnum]

//     return labelEnum
//   }

//   const scroll = (direction) => {
//     const container = containerRef.current;
//     if (!container) return;

//     const scrollAmount = container.offsetWidth ; // Scroll by half the visible area
//     const newScrollPosition =
//       direction === "left"
//         ? container.scrollLeft - scrollAmount
//         : container.scrollLeft + scrollAmount;

//     container.scrollTo({
//       left: newScrollPosition,
//       behavior: "smooth",
//     });
//   };

//   return (
//     <div className="tabs-container">
//       {isLeftArrowVisible && (
//         <button className="arrow left-arrow" onClick={() => scroll("left")}>
//           <img className="left-nav-arrow" src={rightArrowBlue}/>
//         </button>
//       )}

//       <div className="tabs-wrapper" ref={containerRef} onScroll={handleScroll}>
//         {loanList.map((tab, index) => (
//           <div
//             key={index}
//             className={`tab ${
//               selectedLoan?.loanAccountNumber === tab.loanAccountNumber
//                 ? "selected-loan-tab"
//                 : ""
//             }`}
//             onClick={()=>handleTabClick(tab)}
//           >
//             <div className="loan-tab-content">
//               <img src={tab.logo} alt={`company Logo`} className="tab-logo" />
//               <div className="tab-details">
//                 <div className="loan-amount">
//                 <span className="loan-heading loan-account-number-lable">LAN</span>
//                 <h3 className="loan-account-number">{tab.loanAccountNumber}</h3>
//                   <span className="loan-heading">Loan Amount</span>
//                   <h3>{tab.loanAmount}</h3>
//                   <span className="loan-heading">Tenure</span>
//                   <h3>{tab.loanTenure}</h3>
//                 </div>
//               </div>
//               <div className={`status ${tab.status.toLowerCase()}`}>
//                 {getStatusLabel(tab.status)}
//               </div>
//             </div>
//           </div>
//         ))}
//       </div>

//       {(isRightArrowVisible && loanList?.length>1 ) && (
//         <button className="arrow right-arrow" onClick={() => scroll("right")}>
//           <img src={rightArrowBlue}/>
//         </button>
//       )}
//     </div>
//   );
// };

// export default LoanTabs;
import React from "react";
import Carousel from "../carousel/Carousel"; // Adjust the path as needed
import "./LoanTabs.scss";
import ToolTip from "../toolTip/ToolTip";
import LoanActive from "../../assets/images/LoanActive.svg";
import LoanClosed from "../../assets/images/LoanClosed.svg";
import DefaultLoanLogo from "../../assets/images/trillionloans-logo.jpg";

const LOANS_STATUSES = {
  ACTIVE: "Active",
  CLOSED: "Closed",
  WRITTEN_OFF: "Written off",
  OVERPAID: "Overpaid",
} as const;

const getStatusLabel = (labelEnum: string):string => {
  return LOANS_STATUSES[labelEnum as keyof typeof LOANS_STATUSES] || labelEnum;
};

const formatAmount = (amount: number | string): string => 
  amount?.toLocaleString("en-IN", { minimumFractionDigits: 0 }) ?? 0;

const getDPDClass = (dpd: number): string => {
  if (dpd == 0) return 'dpd-on-time';       // On Time
  if (dpd > 0 && dpd <= 30) return 'dpd-overdue'; // Overdue
  return 'dpd-critical';                     // Critical
};

const getDefaultLogoSrc = () => {
  const logo = DefaultLoanLogo as unknown as { src?: string };
  return logo.src ?? (DefaultLoanLogo as unknown as string);
};

const isValidLogoString = (value: string) =>
  value.startsWith("http") || value.startsWith("/") || value.startsWith("data:");

const getLogoSrc = (logo: unknown) => {
  if (typeof logo === "string") {
    return isValidLogoString(logo) ? logo : getDefaultLogoSrc();
  }
  if (logo && typeof logo === "object" && "src" in logo) {
    const src = (logo as { src?: string }).src;
    return src || getDefaultLogoSrc();
  }
  return getDefaultLogoSrc();
};

interface LoanTab {
  loanAccountNumber: string;
  loanAmount: number | string;
  loanTenure: string;
  status: string;
  logo?: unknown;
  dpdDays?: number;
  nextPaymentDue?: { date: string };
}
interface LoanTabsProps {
  loanList: LoanTab[];
  selectedLoan?: { loanAccountNumber: string };
  handleTabClick: (tab: LoanTab) => void;
}
const LoanTabs = ({ loanList, selectedLoan, handleTabClick }: LoanTabsProps) => {
  const carouselTabs = loanList.map((tab: LoanTab, index: number) => (
    <div
      key={index}
      className={`tab ${
        selectedLoan?.loanAccountNumber === tab.loanAccountNumber
          ? "selected-loan-tab"
          : ""
      }`}
      onClick={() => handleTabClick(tab)}
    >
      {/* <div className="loan-tab-content"> */}
        <div className="branding-container">
          <div className="branding-container-product">
            <img
              src={getLogoSrc(tab.logo)}
              alt="company logo"
              className="tab-logo"
              onError={(event) => {
                const img = event.currentTarget;
                if (img.src !== getDefaultLogoSrc()) {
                  img.src = getDefaultLogoSrc();
                }
              }}
            />
            {/* <p className="prodcut-name">{tab.productName}</p> */}
          </div>
          <div className={`status ${tab.status.toLowerCase()}`}>
            <img
              src={tab.status.toLowerCase() === "active" ? LoanActive.src : LoanClosed.src}
              alt={tab.status}
            />
            {getStatusLabel(tab.status)}
          </div>
        </div>
        <div className="tab-details">
          <div className="loan-amount-card">
            <span className="loan-amount-heading">Loan amount</span>
            <span className="loan-amount-value">₹{formatAmount(tab.loanAmount)}</span>
          </div>
          <div className="loan-info">
            <div className="loan-info-tenure">
              <span className="tenure-heading">Tenure</span>
              <span className="tenure-value">{tab.loanTenure}</span>
            </div>
            <div className="loan-info-account">
              <span className="account-heading">A/C no.</span>
              <span className="account-value">{tab.loanAccountNumber}</span>
            </div>
            {tab.dpdDays ? (
              <div className={`dpd-container ${getDPDClass(tab.dpdDays)}`}>
              {/* <ToolTip text="Days Past Due"> */}
              {/* <div className={`${getDPDClass(tab.dpdDays)}`}>DPD</div> */}
              {/* </ToolTip> */}
              {tab.dpdDays} days overdue
            </div>
            ) : (
              <div className="loan-info-nextDue">
                <span className="nextDue-heading">Next Due</span>
                <span className="nextDue-value">{tab.nextPaymentDue?.date || 'N/A'}</span>
              </div>
            )
            }
          </div>
            {/* <span className="loan-heading loan-account-number-lable">LAN</span>
            <h3 className="loan-account-number">{tab.loanAccountNumber}</h3>
            <span className="loan-heading">Tenure</span>
            <h3>{tab.loanTenure}</h3> */}
        </div>
      {/* </div> */}
    </div>
  ));

  return <Carousel cards={carouselTabs} />;
};

export default LoanTabs;
