import { useSearchParams } from "next/navigation";
import { useRouter } from "next/navigation";
import "./RepayLoans.scss"
import LeftArrow from "../../assets/images/Left-Arrow.png"
import CarretDown from "../../assets/images/carret - down.svg"
import { useState, useEffect } from "react";
import axios from "axios";
import Modal from "../modal/Modal";
import RepayLoanLoader from "../../assets/images/Repay-Loan-Loader.json"
import { Player } from "@lottiefiles/react-lottie-player";
import repaySuccess from "../../assets/images/repaySuccess.png";
import repayFailure from "../../assets/images/repayFailure.png";
import repayProgress from "../../assets/images/repayProgress.png";
import repaySuccessMobile from "../../assets/images/Repay-Success-Mobile.png";
import repayFailureMobile from "../../assets/images/Repay-Failure-Mobile.png";
import repayProgressMobile from "../../assets/images/Repay-Progress-Mobile.png";

interface RepayLoansProps {
    onLogout: () => void;
    setShowNavbar: (value: boolean) => void;
    setFooter: (value: boolean) => void;
    GlobalLeadId: string;
    SetGloablLeadId: (value: string) => void;
  }

interface SplitData {
  enabledCollections?: string[];
  value?: number;
  foreclosureSplit?: { principal: number; interest: number; charges: number; };
  currentDueSplit?: { principal: number; interest: number; charges: number; };
  nextDueSplit?: { principal: number; interest: number; charges: number; };
  losProductKey?: string;
  netForeclosureAmount?: number;
  currentDueAmount?: number;
  nextDueAmount?: number;
}

const RepayLoans = ({
    onLogout,
    setShowNavbar,
    setFooter,
    GlobalLeadId,
    SetGloablLeadId
  }: RepayLoansProps) => {
    const searchParams = useSearchParams();
    const leadId = searchParams?.get("leadID") ?? '';
    const loanAccountNumber = searchParams?.get("loanAccount") ?? '';
    const numericLoanAccount = Number(loanAccountNumber);
    const isPaymentModal = searchParams?.get("payment_modal") === "true";
    const [isOpen, setIsOpen] = useState(false);
    
    const [selectedOption, setSelectedOption] = useState<{label: string, value: string} | null>(null);
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [isPolling, setIsPolling] = useState(false);
    const [collectionStatus, setCollectionStatus] = useState<string>("");
    const [statusMessage, setStatusMessage] = useState<string>("");
    const [paymentId, setPaymentId] = useState<string | null>(null);
    const [collectionDetails, setCollectionDetails] = useState<SplitData | null>(null);
    const [errorMessage, setErrorMessage] = useState("");
    const [apiResponse, setApiResponse] = useState<{amount?: number} | null>(null)
    const [repaySplits, setRepaySplits] = useState<{foreclosureSplit?: any, currentDueSplit?: any, nextDueSplit?: any} | null>(null);
    const [isCreatingPayment, setIsCreatingPayment] = useState(false);
    const isFinalStatus = !isPolling && Boolean(collectionStatus);

    const baseUrl = process.env.NEXT_PUBLIC_CUSTOMER_PORTAL_ENDPOINT;

    const options = [
        { label: "Foreclosure", value: "FULL_AMOUNT" },
        { label: "Current Due Installment", value: "CURRENT_DUE_AMOUNT" },
        { label: "Next Due Installment", value: "NEXT_DUE_AMOUNT" },
    ];

    const SPLIT_LABELS = {
        principalDue: "Principal Due",
        interestDue: "Interest Due",
        chargesDue: "Charges Due",
    };
    
    const authHeader = {
        withCredentials: true,
    };

    const router = useRouter();

    const formatAmount = (amount: number | string): string => 
        amount?.toLocaleString("en-IN", { minimumFractionDigits: 0 }) ?? 0;
      

    const filteredOptions = options.filter(opt =>
        collectionDetails?.enabledCollections?.includes(opt.value)
    );    

    const getActiveSplit = () => {
        if (!selectedOption || !repaySplits) return null;
      
        switch (selectedOption.value) {
          case "FULL_AMOUNT":
            return repaySplits.foreclosureSplit;
          case "CURRENT_DUE_AMOUNT":
            return repaySplits.currentDueSplit;
          case "NEXT_DUE_AMOUNT":
            return repaySplits.nextDueSplit;
          default:
            return null;
        }
      };

    const formatNextDueDate = (date: string): string => {
        if (!date) return '';
        return date.replace(/\//g, "-"); // DD/MM/YYYY → DD-MM-YYYY
    };

    const storedNextPaymentDue = localStorage.getItem("nextPaymentDue");

    const nextPaymentDue = storedNextPaymentDue? JSON.parse(storedNextPaymentDue): null;

    const activeSplit = getActiveSplit();
    const principal = activeSplit?.principal ?? 0;
    const interest = activeSplit?.interest ?? 0;
    const charges  = activeSplit?.charges ?? 0;
    const totalAmount = activeSplit
    ? Object.values(activeSplit).reduce((sum: number, val: any) => sum + (val ?? 0), 0)
    : 0;

    const handleSelect = (option: {label: string, value: string}) => {
        setSelectedOption(option);
        setIsOpen(false); // close dropdown
    };

    const handlePaymentRedirect = async () => {
        if (!selectedOption || !collectionDetails) return;
        if (isCreatingPayment) return;
        setIsCreatingPayment(true);
        setErrorMessage(""); // clear any previous error

        try {
            const url = `${baseUrl}/payments/api/v1/customer/${leadId}/loan-account/${numericLoanAccount}/create-payment`;
            // const url = `http://customer-portal.trillionloans.qa/payments/api/v1/customer/13254/loan-account/11599/create-payment`;
            const body = {
              collectionType: selectedOption.value,
            //   productCode:"PLO1",
            ...(collectionDetails?.losProductKey && {
                productCode: collectionDetails.losProductKey,
              }),
            // ONLY for NEXT_DUE_AMOUNT
            ...(selectedOption.value === "NEXT_DUE_AMOUNT" &&
                nextPaymentDue?.date && {
                nextDueDate: formatNextDueDate(nextPaymentDue?.date),
                }),
            };
      
            const res = await axios.post(url, body, authHeader);
            const data = res.data;
            setApiResponse(data)
            // const data = {
            //     "amount": 123.01,
            //     "collectionStatus": "Initiated",
            //     "linkCreatedAt": "2025-09-22T12:35:25+05:30",
            //     "linkExpiryTime": "2025-10-19T15:04:05+05:30",
            //     "paymentId": "alphanumeric string",
            //     "paymentLinkStatus": "ACTIVE",
            //     "returnUrl": "Trillion loader page url with payment id",
            //     "url": "cashfree checkout url"
            // }
            if (data?.url) {
                localStorage.setItem("collectionId", data.collectionId);
                localStorage.setItem("paymentId", data.paymentId);
                localStorage.setItem("paymentAmount", data.amount);
                window.location.href = data.url;
            }else {
                setErrorMessage("Payment link could not be generated. Try again later.");
                setTimeout(() => setErrorMessage(""), 3000);
                setIsCreatingPayment(false);
            }
          } catch (error) {
            console.error("API Error:");
            setErrorMessage("Something went wrong! please try again in 30 mins");
            setTimeout(() => setErrorMessage(""), 3000);
            setIsCreatingPayment(false);
          }
    };

    const getSelectedAmount = () => {
        if (!collectionDetails || !selectedOption) return null;
        if (selectedOption.value === "FULL_AMOUNT") {
          return collectionDetails.netForeclosureAmount;
        } else if (selectedOption.value === "CURRENT_DUE_AMOUNT") {
          return collectionDetails.currentDueAmount;
        }else if (selectedOption.value === "NEXT_DUE_AMOUNT"){
            return collectionDetails.nextDueAmount;
        }
        return null;
    } 
    const handleLogout = () => {
        router.push("/login");
        onLogout();
    };

    const checkLoggedIn = async () => {
        const phoneNumberRaw = localStorage.getItem("phoneNumber");
        if (!phoneNumberRaw) {
          handleLogout();
          return
        }
        try{

            const phoneNumber = JSON.parse(phoneNumberRaw);
            if(!phoneNumber){
                handleLogout();
                return;
            }
        } catch(error){
            console.error("Invalid phoneNumber in localStorage");
            handleLogout();
            return;
        }
    }

    useEffect(() => {
        window.scrollTo({ top: 0, left: 0, behavior: "auto" });
        checkLoggedIn();
        const amount = localStorage.getItem("paymentAmount");
        if (amount) {
          setApiResponse({ amount: Number(amount) });
        }
      }, []);

    useEffect(() => {
        const stored = localStorage.getItem("collectionDetails");
        const storedSplits = localStorage.getItem("repay_splits");
        if (storedSplits) {
            setRepaySplits(JSON.parse(storedSplits));
        }
        if (stored) {
          setCollectionDetails(JSON.parse(stored));
        }
      }, []);

    useEffect(() => {
        if (isPaymentModal) {
            const storedCollectionId = localStorage.getItem("collectionId");
            const storedPaymentId = localStorage.getItem("paymentId");

            // if(!storedCollectionId)return;
            setShowPaymentModal(true);
      
            if (storedPaymentId) setPaymentId(storedPaymentId);
            if (storedCollectionId) {
                startPolling(storedCollectionId);
            }
        }
      }, [searchParams]);      

    const startPolling = (collectionId: string) => {
        setIsPolling(true);
        setStatusMessage("processing");
        setCollectionStatus("");
    
        const minDuration = 10000; // 10 seconds
        const startTime = Date.now();
    
        let prevDelay = 1000;
        let currentDelay = 1000;
        let lastStatus : string | null = null;
        let timeoutId : NodeJS.Timeout | null = null;
    
        const poll = async () => {
        try {
            const res = await axios.get(
            `${baseUrl}/payments/api/v1/loan-account/${numericLoanAccount}/collection-status/${collectionId}`,
            authHeader
            );
    
            const data = res.data;
    
            const status = data?.collectionStatus?.toLowerCase?.();
            if (status) {
            lastStatus = status; // 🔹 SAME AS YOUR LOGIC
            }
    
            const elapsedTime = Date.now() - startTime;
    
            // ✅ STOP CONDITION (replaces maxAttempts)
            if (elapsedTime >= minDuration) {
            finalize(lastStatus);
            return;
            }
    
            // 🔁 Fibonacci delay calculation
            const nextDelay = prevDelay + currentDelay;
            prevDelay = currentDelay;
            currentDelay = nextDelay;
    
            timeoutId = setTimeout(poll, currentDelay);
    
        } catch (error) {
            console.error("Polling Error:");
    
            // 🔹 SAME ERROR HANDLING AS YOUR CODE
            clearTimeout(timeoutId!);
            setIsPolling(false);
            setStatusMessage("Error fetching payment status. Please try again later.");
    
            lastStatus = "initiated";
            setCollectionStatus(lastStatus);
        }
        };
    
        const finalize = (status: string | null) => {
        if(timeoutId)clearTimeout(timeoutId);
        setIsPolling(false);
        setCollectionStatus(status || '');
    
        const paymentAmount =
            apiResponse?.amount || Number(localStorage.getItem("paymentAmount"));
    
        // 🔹 EXACT SAME MESSAGE LOGIC
        if (status === "paid") {
            setStatusMessage(
            `Your payment of ₹ ${paymentAmount} has been received & settled successfully for loan account no. ${numericLoanAccount}`
            );
        } else if (status === "initiated") {
            setStatusMessage(
            `Your payment of ₹ ${paymentAmount} has been received successfully for loan account no. ${numericLoanAccount} , Loan details will be updated in few hours.`
            );
        } else {
            setStatusMessage(
            `Your payment of ₹ ${paymentAmount} has been failed for loan account no. ${numericLoanAccount}, Please try again in sometime`
            );
        }
        };
    
        // 🚀 Start polling
        poll();
    
        // 🧹 Cleanup safety (same intent as before)
        return () => {
            if (timeoutId) clearTimeout(timeoutId);
        }  
    };
  
  
  
    return (
        <div className="repay-loan-container">

            <div className="repay-header">
                <div className="repay-header-content">
                    <div className="repay-loan-arrow-heading">
                        <div className="left-arrow-button">
                        <img
                            src={LeftArrow.src}
                            alt="Back"
                            className="back-arrow"
                            onClick={() => router.push('/home')}
                            />
                        </div>
                        <span>
                            Repay Loan
                        </span>
                    </div>
                </div>
                <div className="repay-divider"></div>
            </div>


            <div className="repay-loan-body-wrapper">
                <div className="repay-loan-body">

                    <div className="repay-loan-subheading">
                        <span>
                            Select Repayment Option for Loan Account No. {numericLoanAccount} 
                        </span>
                    </div>

                    <div className="repay-loan-dropdown-wrapper">
                        <div
                            className={`repay-loan-dropdown ${isOpen || selectedOption ? "open" : ""}`}
                            onClick={() => setIsOpen((prev) => !prev)}
                            >
                            <div className={`dropdown-selected ${selectedOption ? "selected": ""}`}>
                                {selectedOption?.label || "Select Repayment Option"}
                            <img
                                src={CarretDown.src}
                                alt="arrow"
                                className={`dropdown-arrow ${isOpen ? "rotate" : ""}`}
                                />  
                            </div>
                        </div>

                        {isOpen && (
                            <div className="dropdown-options">
                                {filteredOptions.length > 0 ? (
                                    filteredOptions.map((option) => (
                                        <div
                                            key={option.value}
                                            className="dropdown-option"
                                            onClick={() => handleSelect(option)}
                                        >
                                            {option.label}
                                        </div>
                                    ))
                                ) : (
                                    <div className="dropdown-option disabled">No options available</div>
                                )}
                            </div>
                        )}

                    </div>

                </div>


                {activeSplit && (
                <div className="repay-loan-verify-details-section">

                    <div className="repay-loan-verify-details-header">
                        <div className="repay-loan-verify-details-heading">
                            Verify Details
                        </div>
                        <div className="repay-loan-verify-details-subheading">
                            Please verify your loan and payment details before making the payment.
                        </div>
                    </div>

                    <div className="repay-loan-verify-details-header-body">
                        {
                                <>
                                    {Object.entries(activeSplit).map(([key, value]) => (
                                        <div className="repay-loan-verify-details-item" key={key}>
                                    
                                            <div className="repay-loan-verify-details-item1">
                                                <span>{(SPLIT_LABELS as any)[key] ?? key}</span>
                                                {/* mapping and parsing */}
                                            </div>
                                            <div className="repay-loan-verify-details-item2">
                                                <span>₹ {formatAmount(Number(value ?? 0))}</span>
                                            </div>

                                        </div>
                                    ))}

                                    {/* for the total of the three values */}
                                    <div className="repay-loan-verify-details-item total">
                                        <div className="repay-loan-verify-details-item1">
                                            <span>Total Amount Due</span>
                                        </div>
                                        <div className="repay-loan-verify-details-item2">
                                            <span>
                                                ₹ {formatAmount(totalAmount as number)}
                                            </span>
                                        </div>
                                    </div>
                                </>
                        }
                        
                    </div>

                </div>
                    )}

                {selectedOption ? (
                    <div className={`repay-proceed-button ${totalAmount === 0 || isCreatingPayment ? "disabled" : ""}`} onClick={totalAmount > 0 && !isCreatingPayment? handlePaymentRedirect : undefined}>
                    {isCreatingPayment ? "Processing..." : `Proceed to pay ₹ ${formatAmount(getSelectedAmount() as number)}`}
                </div>
             ) : null}
            </div>

        {/* Payment Modal */}
        <Modal isOpen={showPaymentModal} onClose={() => setShowPaymentModal(false)}>
            <div className={`payment-status-modal ${isFinalStatus ? "final-status" : ""}`}>
                {isPolling ? 

                //  if the status api is polling till 30 seconds
                (
                <>
                <div className="payment-status-loading">
                    <Player
                        autoplay
                        loop
                        src={RepayLoanLoader}
                    />
                </div>
                <div className="payment-status-text">
                    <div className="payment-status-header">
                    Please Wait...
                    </div>
                    <div className="payment-status-subtext">
                    We are processing your payment, do not refresh or close this window
                    </div>
                    {/* <div className="payment-status-transactionID">
                    {paymentId ? `Transaction ID: ${paymentId}` : ""}
                    </div> */}
                </div>
                
                </>
                ) : 
                
                // when polling is done and the status can be paid, failed, processing
                (
                <>
                <div className={`payment-status-loading ${isFinalStatus ? "final-status" : ""}`}>
                    {collectionStatus === "paid" && (
                        <img 
                        src={repaySuccess.src}
                        title="success animation"
                        className="status-image status-paid"
                        />
                    )}
                    {collectionStatus === "failed" && (
                        <img
                        src={repayFailure.src}
                        title="failed animation"
                        className="status-image status-failed"
                        />
                    )}
                    {collectionStatus === "initiated" && (
                        <img
                        src={repayProgress.src}
                        title="in-progress animation"
                        className="status-image status-initiated"
                        />
                    )}
                </div>

                {/* Text section */}
                <div className="payment-status-text">
                <div className="payment-status-header">
                    {collectionStatus === "paid"
                    ? "Successfully Done!"
                    : collectionStatus === "failed"
                    ? "Failed!"
                    : "In Progress!"}
                </div>

                <div className="payment-status-subtext">
                    {collectionStatus === "paid" &&
                    // `Your payment of ₹ ${apiResponse.amount} has been received & settled successfully for loan account no. ${loanAccountNumber}`
                    statusMessage
                    }
                    {collectionStatus === "failed" &&
                    // `Your payment of ₹ ${apiResponse.amount} has been received successfully for loan account no. ${loanAccountNumber} , Loan details will be updated in few hours.`
                    statusMessage
                    }
                    {collectionStatus === "initiated" &&
                    // `Your payment of ₹ ${apiResponse.amount} has been failed for loan account no. ${loanAccountNumber}, Please try again in sometime`
                    statusMessage
                    }
                </div>
                <div className="payment-status-transactionID">
                    {paymentId ? `Transaction ID: ${paymentId}` : ""}
                </div>
                </div>

                {/* if myloans cta is available */}
                <div className={`payment-status-cta ${isFinalStatus ? "final-status" : ""}`}>
                    <div className="mylonas-cta"
                    onClick = {()=>{router.push("/home")}}>
                        View My Loans
                    </div>
                </div>

                </>
                )}
            </div>
        </Modal>


        {errorMessage && (
        <div className="payment-error-message">
            {errorMessage}
        </div>
        )}

        </div>
      );
}

export default RepayLoans