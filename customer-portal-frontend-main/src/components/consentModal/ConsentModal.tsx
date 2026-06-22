'use client'

import React, { useState } from "react";
import Modal from "@/components/modal/Modal";
import "./consentModal.scss";

type ConsentModalProps = {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (consent: boolean) => void;
};

const ConsentModal = ({ isOpen, onClose, onSubmit }: ConsentModalProps) => {
  const [isDisabled, setIsDisabled] = useState(false);

  const handleSubmit = () => {
    if(isDisabled)return;
    setIsDisabled(true);
    onSubmit(true);     // user gives consent
  };

  const handleOptOut = () => {
    if(isDisabled)return;
    setIsDisabled(true);
    onSubmit(false);    // user rejects consent
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="consent-wrapper">
        <h2 className="consent-header">Consent for Communication</h2>

        <div className="consent-row-text">
          I authorize Trillionloans Fintech Private Limited to contact me via phone, SMS, email, WhatsApp, or other electronic means for promotional and/or transactional purposes.
        </div>

        <div className="policy-text">
          View Our&nbsp;
          {/* <span> */}
          <a
            href="https://www.bharatpecapital.com/documents/Privacy-Policy.pdf"
            target="_blank"
            rel="noreferrer"
          >
            Privacy Policy
          </a>
          &nbsp;
          {/* </span> */}
          and&nbsp;
          <a
            href="https://www.bharatpecapital.com/policies-codes"
            target="_blank"
            rel="noreferrer"
          >
            Terms of Use
          </a>
          &nbsp;
        </div>


        <div className="consent-button-row">
          <button className="primary-btn" onClick={handleSubmit} type="button">
            Submit
          </button>

          <button
            className="secondary-btn"
            onClick={handleOptOut}
            type="button"
          >
            Opt Out
          </button>
        </div>
      </div>
    </Modal>
  );
};

export default ConsentModal;
