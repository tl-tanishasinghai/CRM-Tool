'use client'

import React, { useEffect, useState } from "react";
import "./DocumentList.scss";
import documentIcon from "@/assets/images/document-icon.svg";
import downloadIcon from "@/assets/images/download-icon.svg";
import axios from "axios";
import Loader from "@/components/loader/Loader";
import { useToast } from "@/components/toast/ToastContext";

type DocumentItem = {
  id: string;
  tagValue: string;
};

type DocumentListProps = {
  loanInfo?: {
    status?: string;
    loanAccountNumber?: string;
    loanApplicationId?: string;
  };
  documentList?: DocumentItem[];
  leadID?: string;
};

export default function DocumentList({
  loanInfo,
  documentList = [],
  leadID = "",
}: DocumentListProps) {
  const { addToast } = useToast();
  const [downloadErrors, setDownloadErrors] = useState(
    new Array(documentList.length + 3).fill("")
  );

  const baseUrl = process.env.NEXT_PUBLIC_CUSTOMER_PORTAL_ENDPOINT;

  useEffect(() => {
    // length increased to 3 for RPS
    setDownloadErrors(new Array(documentList.length + 3).fill(""));
  }, [documentList.length, loanInfo]);

  const downloadDocument = async (
    docName: string,
    id: string,
    docIndex: number
  ) => {
    const apiParam =
      id == "NOC" || id == "SOA" || id == "rps"
        ? loanInfo?.loanAccountNumber
        : loanInfo?.loanApplicationId;
    const apiPath = id == "NOC" || id == "SOA" || id == "rps" ? "document" : "documents";
    // if(id == "RPS")apiPath+="/rps";
    const url = `${baseUrl}/customer/${apiParam}/${apiPath}/${id}${id=="rps"? `/${leadID}`:""}`;
    const header = {
      withCredentials: true,
      // headers: {
      //   Authorization: `Bearer ${
      //     decryptData(
      //       JSON.parse(localStorage.getItem("authToken"))
      //     )
      //   }`,
      //   "X-CSRF-Token": getCSRFToken(), // Include CSRF token in the headers
      // },
    };

    try {
      setDownloadErrors((prev) => {
        const newErrorList = [...prev];
        newErrorList[docIndex] = "loading";
        return newErrorList;
      });
      const response = await axios({
        url: url,
        method: "GET",
        responseType: "arraybuffer",
        ...header,
      });

      const pdfBlob = new Blob([response.data], { type: "application/pdf" });
      const link = document.createElement("a");
      link.href = window.URL.createObjectURL(pdfBlob);
      link.download = `${docName}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      // Clear the loader state upon successful download
      setDownloadErrors((prev) => {
      const newErrorList = [...prev];
      newErrorList[docIndex] = ""; // Remove loader indication
      return newErrorList;
    });
      // setTimeout(() => {
      //   addToast("Download Complete", "success");
      // }, 1000);
     
    } catch (error) {
      setDownloadErrors((prev) => {
        const newErrorList = [...prev];
        newErrorList[docIndex] = "";
        return newErrorList;
      });
      console.error("Error downloading the PDF", error);
        addToast(`${docName} Download Failed`, "error");
    }
  };

  const NOC_STATUSES = ["CLOSED", "OVERPAID"];

  const showNoc = (status?: string) => {
    if (!status) return false;
    return NOC_STATUSES.includes(status);
  };

  const getDocLabelName = (tagName: string) => {
    if (tagName === "Docket") return "Loan Agreement";
    return tagName;
  };

  return (
    <>
      <div className="widget-header">
        <div className="widget-heading">Documents</div>
      </div>
      <div className="document-container">
        <div className="document-card-container">
          <div
            className={`document-card ${
              showNoc(loanInfo?.status) ? "" : "disabled-doc"
            }`}
          >
            <img src={documentIcon.src} alt="Document" className="doc-icon" />
            <span className="doc-name">NOC</span>
            {downloadErrors[0] === "loading" ? (<div className="loader-container-doc"><Loader /></div>) :
            (<img
              src={downloadIcon.src}
              onClick={() => {
                if (showNoc(loanInfo?.status))
                  downloadDocument("NOC", "NOC", 0);
              }}
              alt="Document"
              className={`download-icon ${
                showNoc(loanInfo?.status) ? "" : "disabled-doc"
              }`}
            />)}
          </div>
          { downloadErrors[0] !== 'loading' && (<p className="download-error">{downloadErrors[0]}</p>) }
        </div>
        <div className="document-card-container">
          <div className="document-card">
            <img src={documentIcon.src} alt="Document" className="doc-icon" />
            <span className="doc-name">SOA</span>
            {downloadErrors[1] === "loading" ? (<div className="loader-container-doc"><Loader /></div>) :
            (<img
              src={downloadIcon.src}
              onClick={() => downloadDocument("SOA", "SOA", 1)}
              alt="Document"
              className="download-icon"
            />)
            }
          </div>

          { downloadErrors[1] !== 'loading' && (<p className="download-error">{downloadErrors[1]}</p>) }
        </div>

        {/* here code for the RPS document */}
        <div className="document-card-container">
          <div className="document-card">
            <img src={documentIcon.src} alt="Document" className="doc-icon" />
            <span className="doc-name">RPS</span>
            {downloadErrors[2] === "loading" ? (<div className="loader-container-doc"><Loader /></div>) :
            (<img
              src={downloadIcon.src}
              onClick={() => downloadDocument("RPS", "rps", 2)}
              alt="Document"
              className="download-icon"
            />)
            }
          </div>

          { downloadErrors[2] !== 'loading' && (<p className="download-error">{downloadErrors[2]}</p>) }
        </div>


        {documentList.map((doc, index) => (
          <div className="document-card-container" key={`${doc.id}-${index}`}>
            <div className="document-card">
              <img src={documentIcon.src} alt="Document" className="doc-icon" />
              <span className="doc-name">{getDocLabelName(doc.tagValue)}</span>
              {downloadErrors[index+3] === "loading" ? (<div className="loader-container-doc"><Loader /></div>) :
              (<img
                src={downloadIcon.src}
                onClick={() =>
                  downloadDocument(
                    getDocLabelName(doc.tagValue),
                    doc?.id,
                    index + 3
                  )
                }
                alt="Document"
                className="download-icon"
              />)
              }
            </div>
            { downloadErrors[index+3] !== 'loading' && (<p className="download-error">{downloadErrors[index+3]}</p>) }
          </div>
        ))}
      </div>
    </>
  );
}
