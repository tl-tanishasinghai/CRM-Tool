'use client'

import React from "react";
import "./ContactInfo.scss";
import phoneIcon from "@/assets/images/phone-icon.svg";
import emailIcon from "@/assets/images/email-icon.svg";

const ContactInfo = () => {
  return (
    <>
      <div className="widget-header">
        <div className="widget-heading">Contact us</div>
      </div>
      <div className="contact-info">
        <div className="contact-item">
          <div>
            <div className="label-container">
              <img src={phoneIcon.src} alt="Phone" />
              <div className="contact-label">Phone Number</div>
            </div>
            <a href="tel:02247790096" className="contact-value">
              022-477-90096
            </a>
          </div>
        </div>
        <div className="contact-item">
          <div>
            <div className="label-container">
              <img src={emailIcon.src} alt="Email" />
              <div className="contact-label">Mail</div>
            </div>
            <a
              href="mailto:customercare@trillionloans.com"
              className="contact-value"
            >
              customercare@trillionloans.com
            </a>
          </div>
        </div>
      </div>
    </>
  );
};

export default ContactInfo;
