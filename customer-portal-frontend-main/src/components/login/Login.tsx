'use client'

import React, { useState, useRef, useEffect } from "react";
import "./Login.scss";
import tllogo from "@/assets/images/bharatpe-capital-logo-haorizontal.svg";
import wallet from "@/assets/images/wallet.webp";
import mobileWallet from "@/assets/images/group_mobile.webp"; // Add the mobile wallet image
import axios from "axios";
import { useNavigate } from "@/utils/navigation-compat";
import DOBInput from "@/components/input/dobInput/DOBInput";
import { isValidDate, reverseDate } from "@/utils/dateUtils";
import { useToast } from "@/components/toast/ToastContext";
import { encryptWithAES } from "@/utils/customerPortalEncoder";

const ErrorMessage = ({ message }: {message: string}) => (
  <div className="error-message">
    <span>{message}</span>
  </div>
);
const STEP_1= 'MOBILE'
const STEP_2A = 'PAN'
const STEP_2B = "OTP"

interface LoginProps {
  onLogin: () => void;
  setShowNavbar: (value: boolean) => void;
  setFooter: (value: boolean) => void;
  GlobalLeadId: string;
  SetGloablLeadId: (value: string) => void;
}

const Login = ({ onLogin, setShowNavbar, setFooter, GlobalLeadId, SetGloablLeadId }: LoginProps) => {
  const { addToast } = useToast();
  const { clearToasts } = useToast();
  const [phoneNumber, setPhoneNumber] = useState("");
  const [panInput, setPanInput] = useState("");
  const [showOtpInput, setShowOtpInput] = useState(STEP_1);
  const [otp, setOtp] = useState(new Array(4).fill("")); // Initialize 5-digit OTP array
  const [errorMessage, setErrorMessage] = useState(""); // State for error messages
  const [showSplash, setShowSplash] = useState(false); // State to control splash screen
  const [isLoading, setIsLoading] = useState(false);
  const otpRefs = useRef<HTMLInputElement[]>([]); // Refs for OTP inputs
  const isButtonDisabled = phoneNumber.length !== 10;
  const [resendTimer, setResendTimer] = useState(30);
  const [disableResend, setDisableResend] = useState(true);
  const [dob,setDob]= useState('')

  const baseUrl = process.env.NEXT_PUBLIC_CUSTOMER_PORTAL_ENDPOINT;

  const isInIframe = typeof window !== 'undefined' && window.self !== window.top;

  const navigate = useNavigate();  

  useEffect(() => {
    
    const autToken = localStorage.getItem("authToken");
    if (autToken) {
      isInIframe ? navigate("/embedded-modal") : navigate("/home");
    }
    const isMobileOrTablet = window.matchMedia("(max-width: 767px)").matches;
    setShowNavbar(false);
    setFooter(false);
    if (isMobileOrTablet) {
      // setShowSplash(true);
      setShowSplash(false)
      const timer = setTimeout(() => {
        setShowSplash(false);
      }, 3000); // Display splash screen for 3 seconds

      return () => {
        clearTimeout(timer);
        setShowNavbar(true);
        setFooter(true);
      }; // Cleanup timeout on component unmount
    }
  }, []);

  useEffect(() => {
    const handlePopState = (event: PopStateEvent) => {
      setShowOtpInput(STEP_1);
      navigate("/login");
    };

    window.onpopstate = handlePopState;

    return () => {
      window.onpopstate = null;
    };
  }, [showOtpInput, navigate]);

  useEffect(() => {
    let resendTimeStamp: NodeJS.Timeout | undefined;

    if (showOtpInput==STEP_2B && resendTimer > 0) {
      resendTimeStamp = setInterval(() => {
        setResendTimer((timer) => {
          if (timer == 0) {
            clearInterval(resendTimeStamp);
            setDisableResend(false);
            return 0;
          }
          return timer - 1;
        });
      }, 1000);
    }

    return () => {
      clearInterval(resendTimeStamp);
    };
  }, [showOtpInput, disableResend]);

  const handlePhoneNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const input = e.target.value;
    if (/^\d*$/.test(input) && input.length <= 10) {
      setPhoneNumber(input);
      setErrorMessage(""); // Clear error on valid input
    } else {
      // setErrorMessage("Invalid phone number.");
    }
  };
  const handlePanChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const input =e.target.value.slice(0, 4);
    const filteredPanInput = input.replace(/[^a-zA-Z0-9]/g, '');
    const formattedValue = filteredPanInput.toUpperCase();

    if (formattedValue.length <= 4) {
      setPanInput(formattedValue)
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!/[0-9]/.test(e.key) && e.key !== "Enter") {
      e.preventDefault();
      setErrorMessage("Only numeric values are allowed.");
    }
  };

  const handleGetOtpClick = async () => {
    clearToasts();
    setErrorMessage("");
    if (phoneNumber.length === 10) {
      try {
        setIsLoading(true);
        if(!isValidDate(dob)){
          setErrorMessage("Please enter a valid date");
          return;
        };
        const customerInfo = await axios.post(`${baseUrl}/auth/login`, {
          mobileNumber: phoneNumber,
          dateOfBirth: reverseDate(dob)
        },
        { withCredentials: true });
        localStorage.setItem("phoneNumber", JSON.stringify(phoneNumber));
        if(customerInfo?.data?.flowStatus==='AWAITING_OTP'){
          setShowOtpInput(STEP_2B);
        }else if(customerInfo?.data?.flowStatus==='AWAITING_PAN'){
          setShowOtpInput(STEP_2A);
        }
        setErrorMessage("");
        return;
      } catch (error) {
        // if (error?.response?.status == 404) {
          setErrorMessage((error as any)?.response?.data?.message || 'Something went wrong, Please try again later');
        // }
      } finally {
        setIsLoading(false);
      }
    } else {
      setErrorMessage("Customer not registered.");
    }
  };

  const handleVerifyPan = async () =>{
    setErrorMessage('')
    if(panInput?.length!==4){
      setErrorMessage('Please enter last 4 alphanumeric of your PAN.')
      return;
    }
    try{
      setIsLoading(true);
        const customerInfo = await axios.post(`${baseUrl}/auth/login/verify-pan`, {
          panLast4Digits: panInput,
        },
        { withCredentials: true });
        if(customerInfo?.data?.flowStatus==='AWAITING_OTP'){
          setShowOtpInput(STEP_2B);
        }
        setErrorMessage("");
        return;

    }catch(error){
      
      // setErrorMessage(error?.response?.data?.message || 'Something went wrong, Please try again later')
      addToast((error as any)?.response?.data?.message || 'Something went wrong, Please try again later', "error", null, true);
      navigate("/login");
      setPhoneNumber('')
      setPanInput("")
      // if (error?.status == 400) {
        localStorage.clear();
        setErrorMessage('')
        setShowOtpInput(STEP_1);
      // }
    }finally{
      setIsLoading(false);
    }
  }

  const handleOtpChange = (element: HTMLInputElement, index: number) => {
    const newOtp = [...otp];
    newOtp[index] = element.value;
    setOtp(newOtp);

    // Move to next input box if current input is filled
    if (element.value && index < otp.length - 1) {
      otpRefs.current[index + 1].focus();
    }
  };

  const handleOtpKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, index: number) => {
    if (e.key === "Backspace" && !otp[index] && index > 0) {
      otpRefs.current[index - 1].focus();
    }
  };

  const handleResendOtpClick = async () => {
    setErrorMessage("");
    setOtp(new Array(4).fill(""));
    try {
      const resend = await axios.post(`${baseUrl}/auth/resend`, {
        mobileNumber: phoneNumber,
      },
      { withCredentials: true });

      // setShowOtpInput(true);
      setResendTimer(30);
      setDisableResend(true);
    } catch (error) {
      if ((error as any)?.response?.status == 410) {
        sendOtp();
      } else {
        setErrorMessage((error as any)?.response?.data?.message  || 'Something went wrong, Please try again later');
      }
    }
  };

  const sendOtp = async () => {
    try {
      const resend = await axios.post(`${baseUrl}/auth/send`, {
        mobileNumber: `91${phoneNumber}`,
      },
      { withCredentials: true });

      // setShowOtpInput(true);
      setResendTimer(30);
      setDisableResend(true);
    } catch (error) {
      // if (error?.response?.status == 404) {
        setErrorMessage((error as any)?.response?.data?.message || 'Something went wrong, Please try again later');
      // }
    }
  };

  const handleOtpSubmit = async () => {
    if (!otp.some((digit) => !digit)){
      try {
        setIsLoading(true);
        const authInfo = await axios.post(
          `${baseUrl}/auth/verify`,
          { mobileNumber: phoneNumber, otp: otp.join("") },
          { withCredentials: true }
        );
        localStorage.removeItem("authToken");
        localStorage.setItem(
          "authToken",
          JSON.stringify(authInfo?.data?.token)
        );
        localStorage.setItem("GlobalLeadId",encryptWithAES(authInfo?.data?.leadId?.toString()) )
        // SetGloablLeadId(authInfo?.data?.leadId)
        onLogin();
        isInIframe ? navigate("/embedded-modal"):navigate("/home");
        addToast("Login Successful", "success");



        return;
      } catch (error) {
        setOtp(new Array(4).fill(""));
        if ((error as any)?.response?.status == 404) {
          setErrorMessage((error as any)?.response?.data?.message);
        } else {
          setErrorMessage((error as any)?.response?.data?.message || "Error occured");
        }
      } finally {
        setIsLoading(false);
      }
    } else {
      setErrorMessage(""); // Clear error // Call the onLogin function to update the login state
    }
  };

  return (
    <div className="login-container">
      {showSplash ? (
        <div className="splash-screen">
          <div className="feat-logo">
            <img src={tllogo.src} className="login-tl-logo" alt="Trillionloans logo" />
            <div className="verticle-line"></div> A BharatPe Group Company
          </div>
          <div className="branding">
            <p className="company-tagline">
              Transforming <span className="lending-size">Lending</span>. <br />
              Technology at Core.
            </p>
          </div>
          <img
            src={mobileWallet.src}
            className="mobile-overlay-image"
            alt=""
          />
        </div>
      ) : (
        <>
          {!showSplash && (
            <div className="login-left">
              <img src={wallet.src} className="overlay-image" alt="" />
              <div className="feat-logo">
                <img src={tllogo.src} className="login-tl-logo" alt="Trillionloans logo" />
                <div className="verticle-line"></div> A BharatPe Group Company
              </div>
              <div className="branding">
                <p className="company-tagline">
                  Transforming <span className="lending-size">Lending</span>.{" "}
                  <br />
                  Technology at Core.
                </p>
              </div>
            </div>
          )}
          <div className="login-right">
            <div
              className={`login-box ${showOtpInput==STEP_2B ? "fade-out" : "fade-in"}`}
            >
              {
                showOtpInput==STEP_2A ? (
                  <>
                    <div className="login-cont-one">
                      <h2>Verify PAN</h2>
                      <p>Please enter last four alphanumeric digits of your PAN to proceed further</p>
                    </div>
                    <div className="phone-number-section">
                      <label htmlFor="phone-number">Last four digits of PAN </label>
                      <div className="phone-input">
                  
                        <input
                          type="text"
                          id="pan"
                          placeholder="Last four digits of PAN "
                          value={panInput}
                          onChange={handlePanChange}
                          // onKeyPress={handleKeyPress}
                        />
                      </div>
                      {errorMessage && <ErrorMessage message={errorMessage} />}
                    </div>
                      <button
                        className={`get-top-btn otp-button ${
                          isButtonDisabled || isLoading ? "disabled" : ""
                        }`}
                        disabled={ isLoading}
                        onClick={handleVerifyPan}
                      >
                        Verify
                      </button>
                  </>
                )
              
              : showOtpInput==STEP_1 ? (
                <>
                  <div className="login-cont-one">
                    <h2>Login</h2>
                    <p>Login using a registered mobile number {isInIframe ? "to raise your concern" : "."}</p>
                  </div>
                  <div className="phone-number-section">
                    <label htmlFor="phone-number">Mobile Number</label>
                    <div className="phone-input">
                      <select aria-label="Country code">
                        <option>+91</option>
                      </select>
                      <input
                        type="text"
                        id="phone-number"
                        placeholder="Mobile Number"
                        value={phoneNumber}
                        onChange={handlePhoneNumberChange}
                        onKeyPress={handleKeyPress}
                      />
                    </div>
                    <label htmlFor="dob-day">Date of Birth</label>
                    <DOBInput error="" onChange={(dob: string)=>setDob(dob)} />
                    <p className="dob-sublabel">Please enter DOB as per KYC</p>
                    {errorMessage && <ErrorMessage message={errorMessage} />}
                  </div>
                    <button
                      className={`get-top-btn otp-button ${
                        isButtonDisabled || isLoading ? "disabled" : ""
                      }`}
                      disabled={isButtonDisabled || isLoading}
                      onClick={handleGetOtpClick}
                    >
                      Get OTP
                    </button>
                </>
              ) : (
                <>
                  <div style={{ margin: "32px 0" }}>
                    <h2>Enter OTP</h2>
                    <p>Enter OTP sent to your registered mobile number.</p>
                    <div className="border-bottom"></div>
                  </div>
                  <div
                    className="otp-input-section"
                    style={{ margin: "10px 0", gap: "16px" }}
                    role="group"
                    aria-label="OTP input"
                  >
                    {otp.map((_, index) => (
                      <input
                        key={index}
                        type="text"
                        maxLength={1}
                        value={otp[index]}
                        onChange={(e) => handleOtpChange(e.target, index)}
                        onKeyDown={(e) => handleOtpKeyDown(e, index)}
                        ref={(el) => { if(el) otpRefs.current[index] = el;}}
                      />
                    ))}
                  </div>
                  <div className="resend-otp">
                    <span>Didn't receive code</span>
                    <button
                      className={`resend-btn ${
                        disableResend ? "disbaled-cursor" : ""
                      }`}
                      disabled={disableResend}
                      onClick={handleResendOtpClick}
                    >
                      Resend
                    </button>{" "}
                    {resendTimer != 0 && (
                      <span>
                        in 00:{resendTimer < 10 ? 0 : ""}
                        {resendTimer}
                      </span>
                    )}
                    <p>Security code is valid for 1 minute only</p>
                  </div>
                  {errorMessage && <ErrorMessage message={errorMessage} />}
                  <div style={{ marginBottom: "32px" }}>
                    {/* Placeholder for icon and typography */}
                    <div style={{ height: "50px" }}></div>
                  </div>
                  <div>
                    {/* Placeholder for small popup message */}
                    <div style={{ height: "20px", marginBottom: "12px" }}></div>
                    <button
                      className={`otp-button ${
                        otp.some((element) => element === "") || isLoading
                          ? "disabled"
                          : ""
                      }`}
                      disabled={
                        otp.some((element) => element === "") || isLoading
                      }
                      onClick={handleOtpSubmit}
                    >
                      Login
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default Login;
