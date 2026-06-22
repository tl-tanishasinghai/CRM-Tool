'use client'

import React from "react";
import "./ErrorScreen.scss";

type ErrorScreenProps = {
  logout: () => void;
};

const ErrorScreen = ({ logout }: ErrorScreenProps) => {
  return (
    <div className="main-container">
      <div className="content-container">
        <img alt="Error" />
        <p>Something went wrong</p>
        <button className="error-cta" onClick={logout} type="button">
          Try Again
        </button>
      </div>
    </div>
  );
};

export default ErrorScreen;
