'use client'

import React, { useEffect, useState } from "react";
import axios from "axios";
import "./FreshDeskModal.css";
import uploadIcon from "@/assets/images/upload.svg";
import Loader from "@/components/loader/Loader";

type CustomerDetails = {
  mobileNo?: string;
  email?: string;
  panNumber?: string;
  leadId?: string;
  loanAccounts?: string[];
  // name?: string;
};

type ErrorsState = {
  mobile: boolean;
  email: boolean;
  pan: boolean;
  description: boolean;
  files: boolean | "InvalidExtension";
  category: boolean;
  loanId: boolean;
};

type FreshDeskModalProps = {
  customerDetails: CustomerDetails;
};

const initialErrors: ErrorsState = {
  mobile: false,
  email: false,
  pan: false,
  description: false,
  files: false,
  category: false,
  loanId: false,
};

const FreshDeskModal = ({ customerDetails }: FreshDeskModalProps) => {
  const [mobile, setMobile] = useState("");
  const [email, setEmail] = useState("");
  const [category, setCategory] = useState("");
  const [categories, setCategories] = useState<string[]>([]);
  const [description, setDescription] = useState("");
  const [loanId, setLoanId] = useState("");
  const [loanOptions, setLoanOptions] = useState<string[]>([]);
  const [pan, setPan] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [showError, setShowError] = useState(false);

  const [errors, setErrors] = useState<ErrorsState>(initialErrors);

  const MAX_FILE_COUNT = 5;
  const MAX_FILE_SIZE_MB = 10;
  const baseUrl = process.env.NEXT_PUBLIC_CUSTOMER_PORTAL_ENDPOINT;

  const authHeader = {
    withCredentials: true
  };

  const getTodayDate = () => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  const fetchData = async () => {
    try{
      const categoryRes = await axios.get(`${baseUrl}/customer/support/tickets/categories`, authHeader);
      setCategories(categoryRes.data.categories);
    }
    catch (err) {
      console.error(err);
      setShowError(true);
      setTimeout(() => setShowError(false), 5000);
    }
  }

  useEffect(() => {
    fetchData();
    setLoanOptions(customerDetails.loanAccounts ?? []);
    setMobile(customerDetails?.mobileNo || "");
    setEmail(customerDetails?.email || "");
    setPan(customerDetails?.panNumber?.toUpperCase() || "");
  }, [customerDetails]);
  
  useEffect(() => {
    const handleOffline = () => {
      setShowError(true);
      setTimeout(() => setShowError(false), 5000);
    };
  
    window.addEventListener("offline", handleOffline);
  
    return () => {
      window.removeEventListener("offline", handleOffline);
    };
  }, []);

  
  const isValidMobile = /^\d{10}$/.test(mobile);
  const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  const isValidPan = /^[A-Z]{5}[0-9]{4}[A-Z]$/.test(pan);
  const hasHtmlTags = /<.*?>/.test(description);

  const allowedExtensions = ["pdf", "jpg", "png"];
    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFiles = Array.from(e.target.files ?? []);
        const MAX_TOTAL_SIZE = MAX_FILE_SIZE_MB * 1024 * 1024;

        const invalidExtensionFiles: File[] = [];
        const allowedMimeTypes = ["application/pdf", "image/jpeg", "image/png"];

        // for extensions
        selectedFiles.forEach((file) => {
          const ext = file.name.split(".").pop()?.toLowerCase();
          const isExtValid = ext ? allowedExtensions.includes(ext) : false;
          const isMimeValid = allowedMimeTypes.includes(file.type);
          if (!(isExtValid && isMimeValid)) {
            invalidExtensionFiles.push(file);
          }
        });

        // Show error if any disallowed files were skipped
        if (invalidExtensionFiles.length > 0) {
          setErrors((prev) => ({ ...prev, files: "InvalidExtension" }));
          setTimeout(() => {
            setErrors((prev) => ({ ...prev, files: false }));
          }, 2000);
          return;
        }

        const oversizedFiles = selectedFiles.filter(
          (file) => file.size > MAX_TOTAL_SIZE
        );
        
        if (oversizedFiles.length > 0) {
          setErrors((prev) => ({
            ...prev,
            files: true, // Exceeds file count
          }));
          setTimeout(() => {
            setErrors((prev) => ({ ...prev, files: false }));
          }, 2000); //  Hide after 2 seconds
          return;
        }

        const validFiles = selectedFiles.filter(
          (file) => file.size <= MAX_TOTAL_SIZE
        );
      
        const combined = [...files, ...validFiles];
        const totalSize = combined.reduce((acc, file) => acc + file.size, 0);

        if (combined.length > MAX_FILE_COUNT || totalSize > MAX_TOTAL_SIZE) {
          setErrors((prev) => ({
            ...prev,
            files: true, // Exceeds file count
          }));
          setTimeout(() => {
            setErrors((prev) => ({ ...prev, files: false }));
          }, 2000); //  Hide after 2 seconds
          return;
        }
      
        setFiles(combined);
        setErrors((prev) => ({ ...prev, files: false })); // ✅ Clear error if all is good
      };

  const removeFile = (index: number) => {
    const updated = [...files];
    updated.splice(index, 1);
    setFiles(updated);
  };

const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault();

  if (!navigator.onLine) {
    setShowError(true);
    setTimeout(() => setShowError(false), 5000);
    return; // 💥 Don't submit if offline
  }

  if (isSubmitting) return;
  const newErrors: ErrorsState = {
    mobile: !isValidMobile,
    email: !isValidEmail,
    pan: !isValidPan,
    description: description.trim() === "" || description.trim().length < 30 || hasHtmlTags,
    files: false,
    category: category === "",
    loanId: false,
    // loanId === ""
  };
  setErrors(newErrors);

  if (Object.values(newErrors).some((val) => val)) return;

  setIsSubmitting(true); // ⏳ Show loader while submitting
  setShowSuccess(false);

  try {
    const encodeFiles = async () => {
      const promises = files.map((file) => {
        return new Promise<{ fileName: string; fileContent: string }>(
          (resolve, reject) => {
          const reader = new FileReader();
          reader.onloadend = () => {
            const base64Content = reader.result?.toString().split(",")[1];
            if (!base64Content) {
              reject(new Error("Failed to read file content."));
              return;
            }
            const extension = file.name.split(".").pop()?.toUpperCase() || "";
            const nameWithoutExt = file.name.slice(0, -(extension.length + 1));
            const finalName = `${nameWithoutExt}.${extension}`; // ⬅ ensure extension is uppercase
            resolve({
              fileName: finalName,
              fileContent: base64Content
            });
          };
          reader.onerror = reject;
          reader.readAsDataURL(file);
        });
      });
      return Promise.all(promises);
    };

    const attachments = files.length > 0 ? await encodeFiles() : [];

    const payload = {
      registeredMobileNumber: `+91${mobile}`,
      email,
      concernCategory: category,
      description,
      loanId,
      panCard: pan,
      attachments,
      // customerName: customerDetails?.name || "",
      // submissionDate: getTodayDate()
    };

    await axios.post(
      `${baseUrl}/customer/support/tickets/form/${customerDetails?.leadId}`,
      payload,
      authHeader
    );
    
    //You can optionally reset form state or trigger toast here
    // Reset form state
    setMobile('');
    setEmail('');
    setCategory('');
    setDescription('');
    setLoanId('');
    setPan('');
    setFiles([]);
    setErrors(initialErrors);
    
    // ✅ Trigger success message
    setShowSuccess(true);
    // ✅ Optionally auto-hide after delay
    setTimeout(() => setShowSuccess(false), 5000);
  } catch (err) {
    console.error(err);
    setShowError(true);
    setTimeout(() => setShowError(false), 5000);
  } finally {
    setIsSubmitting(false); // Reset loader
  }
};

  return (
    <form className="freshdesk-form" onSubmit={handleSubmit}>
      <h2>Raise a Concern</h2>

      <label htmlFor="mobile-input" className="label-tight" style={{ display: 'block'}}>
        Registered Mobile Number <span style={{ color: 'red' }}>*</span>
        </label>
      <input
        type="text"
        id="mobile-input"
        placeholder="Enter Registered Mobile Number *"
        value={mobile}
        onChange={(e) => setMobile(e.target.value)}
        disabled={!!customerDetails?.mobileNo}
      />
      {errors.mobile && <p className="error">Enter a 10-digit mobile number</p>}

      <label htmlFor="email-input" className="label-tight" style={{ display: 'block' }}>
        Email Address <span style={{ color: 'red' }}>*</span>
        </label>
      <input
        type="email"
        id="email-input"
        placeholder="Enter Email ID *"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        disabled={!!customerDetails?.email}
      />
      {errors.email && <p className="error">Enter a valid email address</p>}

      <label htmlFor="category-select" className="label-tight" style={{ display: 'block', marginBottom: '6px' }}>
        Concern Category <span style={{ color: 'red' }}>*</span>
        </label>
      <select id="category-select" value={category} onChange={(e) => setCategory(e.target.value)}>
        <option value="">Select Concern Category</option>
        {categories.map((cat) => (
          <option key={cat} value={cat}>{cat}</option>
        ))}
      </select>
      {errors.category && <p className="error">Please select a concern category</p>}

      <label htmlFor="description-textarea" className="label-tight" style={{ display: 'block', marginBottom: '6px' }}>
        Description <span style={{ color: 'red' }}>*</span>
    </label>
      <textarea
        id="description-textarea"
        placeholder="Enter Description *"
        rows={3}
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      {errors.description && <p className="error">Description cannot be empty, must not contain HTML tags, and it must be at least 30 characters</p>}

      <label htmlFor="loan-id-select" className="label-tight" style={{ display: 'block', marginBottom: '6px' }}>
        Loan ID 
        </label>
      <select id="loan-id-select" value={loanId} onChange={(e) => setLoanId(e.target.value)}>
        <option value="">Select Loan ID</option>
        {loanOptions.map((id) => (
          <option key={id} value={id}>{id}</option>
        ))}
      </select>
      {errors.loanId && <p className="error">Please select a loan ID</p>}

      {/* <label htmlFor="pan-input" className="label-tight" style={{ display: 'block' }}>
        PAN Card Number <span style={{ color: 'red' }}>*</span>
        </label>
      <input
        type="text"
        id="pan-input"
        placeholder="Enter PAN Number*"
        value={pan}
        onChange={(e) => setPan(e.target.value.toUpperCase())}
        disabled={!!customerDetails?.panNumber}
      /> */}
      {errors.pan && <p className="error">Enter a valid PAN (ABCDE1234F)</p>}
      <p className="attachment-note">Enter Upto 5 Attachments. Max Size 10 MB</p>
      <div className="upload-section">
        <label className="upload-box">
          <img src={uploadIcon.src} alt="Upload" className="upload-icon" />
          {/* <div className="upload-icon">⬆️</div> */}
          <input type="file" multiple hidden onChange={handleFileChange} />
          <span>Choose Files To Upload</span>
        </label>

        <ul className="uploaded-files">
          {files.map((file, idx) => (
            <li key={`${file.name}-${file.size}-${idx}`}>
              {file.name}
              <button type="button" onClick={() => removeFile(idx)}>
                ✖
              </button>
            </li>
          ))}
        </ul>
      </div>

      {errors.files === true && <p className="error">You can upload upto {MAX_FILE_COUNT} attachments only with total size under {MAX_FILE_SIZE_MB} MB.</p>}
      {errors.files === "InvalidExtension" && (<p className="error">Only PDF, JPG, and PNG Files Are Allowed.</p>)}
      {/* {errors.files === "LimitExceeded" && (<p className="error">Upload up to {MAX_FILE_COUNT} files, with total size under {MAX_FILE_SIZE_MB} MB.</p>)} */}
      <p className="upload-note">*Mandatory Fields</p>
      {/* {showSuccess && (<p id="form-success-msg" style={{alignSelf:'center', color: 'green', marginTop: '8px' }}>Form Submitted Successfully!</p>)} */}
      {showSuccess && (
        <p data-testid="form-success-msg" style={{ alignSelf: "center", color: "green", marginTop: "8px", fontSize: "13px" }}>
          Form Submitted Successfully!
        </p>
      )}
      {showError && (
        <p style={{ alignSelf: "center", color: "red", marginTop: "8px", fontSize: "13px" }}>
          Something went wrong while submitting. Please try again.
        </p>
      )}

      <button type="submit" className="submit-btn" disabled={isSubmitting}>
      {isSubmitting ? "Submitting..." : "Submit"}
      </button>

    </form>
  );
};

export default FreshDeskModal;
