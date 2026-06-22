'use client'

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import LoanTabs from "./LoanTabs";

// Mock Carousel to just render children
jest.mock("../carousel/Carousel", () => ({ cards }) => (
  <div data-testid="mock-carousel">{cards}</div>
));

const mockLoans = [
  {
    loanAccountNumber: "LAN123",
    loanAmount: "₹1,00,000",
    loanTenure: "12 months",
    logo: "logo.png",
    status: "ACTIVE",
  },
  {
    loanAccountNumber: "LAN456",
    loanAmount: "₹2,00,000",
    loanTenure: "24 months",
    logo: "logo.png",
    status: "CLOSED",
  },
];

const mockHandleTabClick = jest.fn();

describe("LoanTabs component", () => {
  it("renders all loan tabs inside Carousel", () => {
    render(
      <LoanTabs
        loanList={mockLoans}
        selectedLoan={mockLoans[0]}
        handleTabClick={mockHandleTabClick}
      />
    );

    expect(screen.getByText("LAN123")).toBeInTheDocument();
    expect(screen.getByText("LAN456")).toBeInTheDocument();
    expect(screen.getAllByText("Loan Amount")).toHaveLength(2);
    expect(screen.getAllByText("Tenure")).toHaveLength(2);
    expect(screen.getByText("Active")).toBeInTheDocument();
    expect(screen.getByText("Closed")).toBeInTheDocument();
  });

  it("highlights the selected loan tab", () => {
    render(
      <LoanTabs
        loanList={mockLoans}
        selectedLoan={mockLoans[1]}
        handleTabClick={mockHandleTabClick}
      />
    );

    const selectedTab = screen.getByText("LAN456").closest(".tab");
    expect(selectedTab).toHaveClass("selected-loan-tab");
  });

  it("calls handleTabClick when a tab is clicked", () => {
    render(
      <LoanTabs
        loanList={mockLoans}
        selectedLoan={mockLoans[0]}
        handleTabClick={mockHandleTabClick}
      />
    );

    fireEvent.click(screen.getByText("LAN456"));
    expect(mockHandleTabClick).toHaveBeenCalledWith(mockLoans[1]);
  });
});
