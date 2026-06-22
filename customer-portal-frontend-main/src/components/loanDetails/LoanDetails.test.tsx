'use client'

import React from "react";
import { render, screen } from "@testing-library/react";
import LoanDetails from "./LoanDetails";

// Sample props
const mockProps = {
  loanGridDetails: [
    { type: "image", src: "logo.png", alt: "Logo", className: "loan-details-logo" },
    { type: "label-value", label: "Loan Amount", value: "₹50,000", className: "loan-amount" },
    { type: "status", value: "ACTIVE", className: "status-box active" }
  ],
  loanRowDetails: [
    { label: "Tenure", value: "12 months" },
    { label: "Installment Amount", value: "₹4,500" },
  ],
  additionalDetails: [
    { label: "Net Disbursement Amount", value: "₹48,500" },
    { label: "LSP", value: "ABC Finance" },
  ]
};

describe("LoanDetails Component", () => {
  test("renders the Loan Details heading", () => {
    render(<LoanDetails {...mockProps} />);
    expect(screen.getByText("Loan Details")).toBeInTheDocument();
  });

  test("renders logo image", () => {
    render(<LoanDetails {...mockProps} />);
    const image = screen.getByAltText("Logo");
    expect(image).toBeInTheDocument();
    expect(image).toHaveClass("loan-details-logo");
  });

  test("renders loan amount label and value", () => {
    render(<LoanDetails {...mockProps} />);
    expect(screen.getByText("Loan Amount")).toBeInTheDocument();
    expect(screen.getByText("₹50,000")).toBeInTheDocument();
  });

  test("renders loan status text correctly", () => {
    render(<LoanDetails {...mockProps} />);
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  test("renders main loan row details", () => {
    render(<LoanDetails {...mockProps} />);
    expect(screen.getByText("Tenure")).toBeInTheDocument();
    expect(screen.getByText("12 months")).toBeInTheDocument();
    expect(screen.getByText("Installment Amount")).toBeInTheDocument();
    expect(screen.getByText("₹4,500")).toBeInTheDocument();
  });

  test("renders additional details", () => {
    render(<LoanDetails {...mockProps} />);
    expect(screen.getByText("Net Disbursement Amount")).toBeInTheDocument();
    expect(screen.getByText("₹48,500")).toBeInTheDocument();
    expect(screen.getByText("LSP")).toBeInTheDocument();
    expect(screen.getByText("ABC Finance")).toBeInTheDocument();
  });
});
