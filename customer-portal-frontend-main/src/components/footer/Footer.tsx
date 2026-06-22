'use client'

import React from 'react';
import "./Footer.scss";
import tlLogo from "@/assets/images/BPC_logo_dark-2.png";

const Footer = () => {
    return (
        <footer className='footer-container'>
            <div className="footer-content">
                <div className="footer-section">
                    <img className="footer-logo" src={tlLogo.src} alt="Trillionloans logo"/>
                    <p>Resilient Finance Private Limited (formerly known as Trillionloans Fintech Private Limited) is an NBFC registered with the Reserve Bank of India (RBI).</p>
                </div>
                <div className="footer-section">
                    <a href='https://www.bharatpecapital.com/lending-partners' target="_self">Lending Partners</a>
                    <a href='https://www.bharatpecapital.com/grievanceRedressalMechanism' target="_self">Grievance Redressal Mechanism</a>
                    <a href='https://sachet.rbi.org.in/' target="_new">Sachet</a>
                    <a href='https://www.bharatpecapital.com/customerAwareness' >Customer Awareness – TRAI Guideline</a>
                </div>
                <div className="footer-section">
                    <p>Legal</p>
                    <a href='https://www.bharatpecapital.com/documents/Privacy-Policy.pdf' target="_new">Privacy Policy</a>
                    <a href='https://www.bharatpecapital.com/documents/Disclaimer.pdf' target="_new">Disclaimer</a>
                    <a href='https://www.bharatpecapital.com/documents/Refund-and-Cancellation-Policy.pdf' target="_new">Refund and Cancellation Policy</a>
                    <a href='https://www.bharatpecapital.com/customer-awareness' target="_self">Customer Awareness</a>
                </div>
            </div>
            <hr className="demarcation-line"></hr>
            <div className="footer-bottom">
                <p>Resilient Finance Private Limited (formerly known as Trillionloans Fintech Private Limited); Corporate Identity Number (CIN): U65100DL2018PTC445221</p>
                <p>&copy; 2026, All Rights Reserved</p>
            </div>
        </footer>
    );
};

export default Footer;
