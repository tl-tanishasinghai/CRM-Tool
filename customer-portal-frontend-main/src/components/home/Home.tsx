'use client'

import React, { useEffect, useState } from "react";
import "./Home.scss";
import LoanDetails from "../loanDetails/LoanDetails";
import LoanTabs from "../loanTabs/LoanTabs";
import TransactionTable from "../transactionsTable/TransactionTable";
import DocumentList from "../documents/DocumentList";
import fetchApiData, { fetchTransactions } from "../../services/Home.service";
import axios from "axios";
import Accordion from "../accordion/Accordion";
import ContactInfo from "../contactInfo/ContactInfo";
import Loader from "../loader/Loader";
import { useRouter } from "next/navigation";
import ErrorScreen from "../errorScreen/ErrorScreen";
import { decryptData } from "../../utils/customerPortalDecoder";
import { FQAS } from "../accordion/Constants";
import formatWithCommas from "../../utils/formatCommas";
import OverlayModal from "../overlay/OverlayModal";
import FreshDeskModal from "../freshdesk/FreshDeskModal";
import { decryptWithAES } from "../../utils/customerPortalEncoder";
import ConsentModal from "../consentModal/ConsentModal";
import { CLIENT_CONSENT_KEY } from "./constants";

interface HomeProps {
  onLogout: () => void;
  setShowNavbar: (value: boolean) => void;
  setFooter: (value: boolean) => void;
  GlobalLeadId?: string;
  SetGloablLeadId?: (value: string) => void;
}

const Home = ({ onLogout, setShowNavbar, setFooter, GlobalLeadId, SetGloablLeadId }: HomeProps) => {
  const [activeTab, setActiveTab] = useState("loans");
  const [loanList, setLoanList] = useState<any[]>([]);
  const [transactionsList, setTransactionsList] = useState<any[]>([]);
  const [customerDetails, setCustomerDetails] = useState<any>({});
  const [selectedLoan, setSelectedLoan] = useState<any>({});
  const [isLoading, setIsLoading] = useState(false);
  const [isLoanDataLoading, setIsLoanDataLoading] = useState(false);
  const [docList, setDocList] = useState([]);
  const [apiError, setApiError] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [selectedStatus, setSelectedStatus] = useState("All");
  const [showConsentModal, setShowConsentModal] = useState(false);
  const router = useRouter();

  const baseUrl = process.env.NEXT_PUBLIC_CUSTOMER_PORTAL_ENDPOINT;
  const consentKey = CLIENT_CONSENT_KEY;
  const loanGridDetails = [
    {
      type: "image",
      className: "loan-details-logo",
      src: selectedLoan?.logo,
      alt: "BharatPe Logo",
    },
    {
      type: "label-value",
      label: "Loan Amount",
      value: formatWithCommas(selectedLoan?.loanAmount, "₹"),
      className: "loan-amount",
    },
    {
      type: "status",
      className: `status-box ${selectedLoan?.status?.toLowerCase()}`,
      value: selectedLoan?.status,
    },
  ];
  
  const loanRowDetails = [
    {
      label: "Tenure",
      value: selectedLoan?.loanTenure,
    },
    {
      label: "Installment Amount",
      value: formatWithCommas(selectedLoan?.emiAmount, "₹"),
    },
    {
      label: "Disbursement Date",
      value: selectedLoan?.disbursementDate,
    },
    {
      label: "ROI",
      value: selectedLoan?.interestRate,
    },
  ];
  
  const additionalDetails = [
    {
      label: "Net Disbursement Amount",
      value: formatWithCommas(selectedLoan?.netDisbursementAmount, "₹"),
    },
    {
      label: "Lending Service Provider (LSP)",
      value: selectedLoan?.lendingServiceProvider,
    },
    {
      label: "Pre Disbursement Charges",
      value: formatWithCommas(selectedLoan?.chargesPreDisbursal, "₹"),
    },
    {
      label: "Post Disbursement Charges",
      value: formatWithCommas(selectedLoan?.chargesPostDisbursal, "₹"),
    },
    selectedLoan?.lastBureauReportingDate && {
      label: "Last Bureau reporting done on",
      value: selectedLoan?.lastBureauReportingDate,
    },
    selectedLoan?.lastPaymentDone?.amount && {
      label: "Last payment amount",
      value: formatWithCommas(selectedLoan?.lastPaymentDone?.amount, "₹"),
    },
    selectedLoan?.lastPaymentDone?.date && {
      label: "Last payment date",
      value: selectedLoan?.lastPaymentDone?.date,
    },
    selectedLoan?.nextPaymentDue?.date && {
      label: "Next payment date",
      value: selectedLoan?.nextPaymentDue?.date,
    },
    selectedLoan?.nextPaymentDue?.amount && {
      label: "Next payment amount",
      value: formatWithCommas(selectedLoan?.nextPaymentDue?.amount, "₹"),
    },
    {
      label: "Total principal outstanding",
      value: formatWithCommas(selectedLoan?.totalPrincipalOutstanding, "₹"),
    },
  ].filter(Boolean)

  
  const handleLogout = () => {
    router.push("/login");
    onLogout();
  };

  const checkConsent = async () => {

      const userConsentGiven = localStorage.getItem("userConsentGiven");
    
      if (userConsentGiven === "true") {
        return; // Skip everything - no modal
      }
      
      try {
        const encryptedLeadId = localStorage.getItem("GlobalLeadId");
        if(!encryptedLeadId){
          return;
        }

        const customerLeadId = decryptWithAES(encryptedLeadId)
        const response = await axios.get(
          `${baseUrl}/customer/getConsent/${customerLeadId}`, authHeader
        );

        const dataConsent = response?.data || {} ;
        // API returns consent: "yes" | "no"
        if (!("consentStatus" in dataConsent) || dataConsent?.consentStatus === false){
          setTimeout(() => {
            setShowConsentModal(true);
          }, 1000);
        }
      } catch (err) {
        console.error("Error checking consent")
      }
    }

  useEffect(() => {
    setShowNavbar(true);
    setFooter(true);

    getUserData();
    checkConsent();
  }, []);

  useEffect(() => {
    if (activeTab == "loans") {
      fetchUserLoans();
    }
  }, []);

  useEffect(() => {
    if(customerDetails?.leadId){
      fetchUserLoans(selectedStatus);
    }
   
  }, [selectedStatus]);

  useEffect(() => {
    if (selectedLoan?.loanAccountNumber) {
      try {
        getDocuments();
        fetchTransactions(
          `${baseUrl}/customer/${selectedLoan?.loanAccountNumber}/transaction-details`,
          authHeader
        )
          .then((res) => {
            setTransactionsList(res as any[]);
          })
          .catch((error) => {
            if (error?.status == 404) {
              setTransactionsList([]);
            }
          });
      } catch (error: any) {
        if (error?.status == 403) {
          handleLogout();
          return;
        }
        setApiError(true);
        console.log("unexpected error occurred");
      }
    }
  }, [selectedLoan]);

  const authHeader = {
    withCredentials: true,
    
  };

  const handleConsentSubmit = async (userChecked: boolean) => {
    try {
      const encryptedLeadId = localStorage.getItem("GlobalLeadId");
        if(!encryptedLeadId){
          return;
        }
      const customerLeadId = decryptWithAES(encryptedLeadId);
      
      const res = await axios.get("https://api.ipify.org?format=json");
      const publicIP = res.data.ip;

      if (!publicIP || !consentKey) {
        setShowConsentModal(false); 
        return
      } else {
        await axios.post(
          `${baseUrl}/customer/saveConsent/${customerLeadId}`,
          {
            consentStatus: userChecked, // boolean
            ipAddress: publicIP, 
            consentKey: consentKey, // from API1
          }, authHeader
        );
      }
    } catch (err) {
      console.error("Error saving consent");
    } finally {
      localStorage.setItem("userConsentGiven", "true");
      setShowConsentModal(false);  // always close after user decision
    }
  };

  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedStatus(e.target.value);
  };
  const getDocuments = () => {
    setDocList([]);
    if (selectedLoan?.loanApplicationId) {
      try {
        axios
          .get(
            `${baseUrl}/customer/${selectedLoan?.loanApplicationId}/documents`,
            authHeader
          )
          .then((res) => {
            setDocList(res?.data);
          })
          .catch((err) => {
            if (err?.status == "404") {
              return;
            } else if (err?.status == "403") {
              handleLogout();
              return;
            }
            setApiError(true);
            console.log("unexpected error occurred");
          });
      } catch (error: any) {
        if (error?.status == "403") {
          handleLogout();
          return;
        }
        setApiError(true);
        console.log("unexpected error occurred");
      }
    }
  };

  const getUserData = async () => {
    const phoneNumberRaw = localStorage.getItem("phoneNumber");
    if (!phoneNumberRaw) {
      handleLogout();
      return
    }

    const phoneNumber = JSON.parse(phoneNumberRaw);
    if (!phoneNumber) {
      handleLogout();
      return
    }

    try {
      setIsLoading(true);
      // const leadIds = await axios.get(
      //   `${baseUrl}/customer/info/${phoneNumber}`,
      //   authHeader
      // );
      // let decryptedLeadIds = await decryptData(leadIds.data);
      // decryptedLeadIds = JSON.parse(decryptedLeadIds);
      // const leadId = decryptedLeadIds?.[0]?.leadId;

      const leadId = localStorage.getItem("GlobalLeadId");
      if (!leadId) {
        setIsLoading(false);
        handleLogout();
        return;
      }
      const customerLeadId = decryptWithAES(leadId)
      const customerInfo = await axios.get(
        `${baseUrl}/customer/${customerLeadId}`,
        authHeader
      );

      const decryptedCustomerInfo = await decryptData(customerInfo?.data as any);
      setCustomerDetails(JSON.parse(decryptedCustomerInfo as string));
      setIsLoading(false);
    } catch (error) {
      setIsLoading(false);
      handleLogout();
    }
  };

  const fetchUserLoans = async (status?: string) => {
    let leadId = customerDetails?.leadId;
    if(!leadId){
      const encryptedLeadId = localStorage.getItem("GlobalLeadId");
      if(!encryptedLeadId){
        return;
      }
      leadId = decryptWithAES(encryptedLeadId);
    }
    setIsLoanDataLoading(true);
    const loansURL = !status || status=="All" ? 'loans' : `loans?status=${status}` 
    if (leadId) {
      try {
        fetchApiData(
          `${baseUrl}/customer/${leadId}/${loansURL}`,
          authHeader
        )
          .then((res:any) => {
            const loans = (res as any[]).filter(
              (loan: any) => {
                return (loan as any).status !== "UNKNOWN_STATUS";
              }
            );
            setLoanList(loans);
            setSelectedLoan(res?.[0] || {});
            setIsLoanDataLoading(false);
          })
          .catch((err) => {
            if (err?.status == 403) {
              handleLogout();
              return;
            }else if(err?.status ==404){
              setLoanList([])
              setIsLoanDataLoading(false);
              setSelectedLoan({})
              return
            }
            setApiError(true);
          });
      } catch (error: any) {
        if (error?.status == 403) {
          handleLogout();
          return;
        }
        else if(error?.status ==404){
          setLoanList([])
          setIsLoanDataLoading(false);
          setSelectedLoan({})

          return
        }
        setApiError(true);
        console.log("unexpected error occurred");
      }
    }
  };

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
  };

  const handleLoanTabChange = (tab: any) => {
    setSelectedLoan(tab);
  };
  if (isLoading) {
    return (
      <div className="loader-container">
        <Loader />
      </div>
    );
  }
  if (apiError) {
    return <ErrorScreen logout={()=>window.location.reload()} />;
  }

  return (
    <div className="home-container">
      <div className="header-top-container">
        <p className="header-top">Welcome, {customerDetails?.name}</p>
      </div>
      <div className="tab-header">
        <button
          className={`tab-button ${activeTab === "profile" ? "active" : ""}`}
          onClick={() => handleTabChange("profile")}
        >
          Profile
        </button>
        <button
          className={`tab-button ${activeTab === "loans" ? "active" : ""}`}
          onClick={() => handleTabChange("loans")}
        >
          Loans
        </button>
      </div>
      <div className="tab-content">
        {activeTab === "profile" && (
          <div className="profile-section">
            <div className="profile-details-top">
              <div className="profile-detail-item">
                <div className="profile-detail-label">Customer ID</div>
                <div className="profile-detail-value">
                  {customerDetails?.leadId}
                </div>
              </div>
              <div className="profile-detail-item">
                <div className="profile-detail-label">Customer Name</div>
                <div className="profile-detail-value">
                  {customerDetails?.name}
                </div>
              </div>

              <div className="profile-detail-item">
                <div className="profile-detail-label">Age</div>
                <div className="profile-detail-value">
                  {customerDetails?.age}
                </div>
              </div>
              <div className="profile-detail-item">
                <div className="profile-detail-label">Date Of Birth</div>
                <div className="profile-detail-value">
                  {customerDetails?.dateOfBirth}
                </div>
              </div>
            </div>
            <div className="profile-details-bottom">
              {customerDetails?.address && <div className="profile-detail-item">
                <div className="profile-detail-label">Permanent Address</div>
                <div className="profile-detail-value">
                  {customerDetails?.address}
                </div>
              </div>}
              <div className="profile-detail-item">
                <div className="profile-detail-label">Registered Email ID</div>
                <div className="profile-detail-value">
                  {customerDetails?.email}
                </div>
              </div>
              <div className="profile-detail-item">
                <div className="profile-detail-label">
                  Registered Mobile Number
                </div>
                <div className="profile-detail-value">
                  {customerDetails?.mobileNo}
                </div>
              </div>
            </div>

            
          </div>
        )}
        {activeTab === "loans" &&
          (isLoanDataLoading ? (
            <div className="loader-container">
              <Loader />
            </div>
          ) : (
            <div className="loans-section">
              <label className="filter-label" htmlFor="loanfilter">Filter Loans:</label>
              <select className="filter-dropdown"
                id="loanfilter"
                value={selectedStatus}
                onChange={handleStatusChange}
              >
                <option value="Active">Active</option>
                <option value="Closed">Closed</option>
                <option value="All">All</option>
              </select>

              <LoanTabs
                loanList={loanList}
                selectedLoan={selectedLoan}
                handleTabClick={handleLoanTabChange}
              />
              {Object.keys(selectedLoan).length !== 0 ? <LoanDetails 
              loanRowDetails={loanRowDetails}
              loanGridDetails={loanGridDetails}
              additionalDetails={additionalDetails}
              leadID={customerDetails?.leadId}
              LAN={selectedLoan?.loanAccountNumber}
              collectionDetails={selectedLoan?.collectionDetails}
              latestCollectionDetails={(transactionsList as any)?.latestCollectionDetails || []}
              currentDueSplit={selectedLoan?.currentDueSplit}
              foreclosureSplit={selectedLoan?.foreclosureSplit}
              nextDueSplit={selectedLoan?.nextDueSplit}
              nextPaymentDue={selectedLoan?.nextPaymentDue}
              // loanInfo={selectedLoan} 
              /> : <div className="no-loans-container"><p className="no-loans">No Loans found</p></div>}
              {Object.keys(selectedLoan).length !== 0 && <TransactionTable transactionsList={(transactionsList as any)?.transactions || []} />}
              {Object.keys(selectedLoan).length !== 0 && <DocumentList loanInfo={selectedLoan} documentList={docList} leadID={customerDetails?.leadId}/>}
              <Accordion accordionList={FQAS?.slice(0, 5)} showAllFaqCta />
              <ContactInfo />
            </div>
          ))}
      </div>

      {!showModal && (<div className="tooltip-wrapper">
      <button
        className="floating-btn"
        onClick={() => setShowModal(true)}
        aria-label="Open Support Modal"
      >
        🙋
      </button>
      <span className="tooltip-text">Raise your Concern</span>
      </div>
      )}

      <OverlayModal isOpen={showModal} onClose={() => setShowModal(false)}>
        <FreshDeskModal customerDetails={customerDetails}/>
      </OverlayModal>

      {/* consent modal */}
      <ConsentModal
        isOpen={showConsentModal}
        onClose={() => setShowConsentModal(false)}
        onSubmit={handleConsentSubmit}
      />
    </div>
  );
};

export default Home;
