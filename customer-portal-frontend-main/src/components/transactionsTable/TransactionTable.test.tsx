'use client'

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import TransactionTable from "./TransactionTable";

const mockTransactions = [
  { date: "2024-01-01", status: "Completed", amount: "₹1,000" },
  { date: "2024-01-02", status: "Completed", amount: "₹2,000" },
];

describe("TransactionTable", () => {
  it("renders heading", () => {
    render(<TransactionTable transactionsList={[]} />);
    expect(screen.getByText("Transactions")).toBeInTheDocument();
  });

  it("shows 'No Transactions found' message when list is empty", () => {
    render(<TransactionTable transactionsList={[]} />);
    expect(screen.getByText("No Transactions found")).toBeInTheDocument();
  });

  it("renders transaction row if data is present", () => {
    render(<TransactionTable transactionsList={mockTransactions} />);
    expect(screen.getByText("View Last 2 transactions")).toBeInTheDocument();
    expect(screen.getByText("2024-01-01")).toBeInTheDocument();
    expect(screen.getByText("₹1,000")).toBeInTheDocument();
  });

  it("opens modal on button click", () => {
    render(<TransactionTable transactionsList={mockTransactions} />);
    fireEvent.click(screen.getByText("View Last 2 transactions"));
    expect(screen.getByText("Last 2 transactions")).toBeInTheDocument();
    expect(screen.getByText("2024-01-02")).toBeInTheDocument();
    expect(screen.getByText("✕")).toBeInTheDocument();
  });

  it("closes modal when ✕ is clicked", () => {
    render(<TransactionTable transactionsList={mockTransactions} />);
    fireEvent.click(screen.getByText("View Last 2 transactions"));
    const closeBtn = screen.getByText("✕");
    fireEvent.click(closeBtn);
    expect(closeBtn).not.toBeVisible(); // Or check: queryByText("✕") === null
  });
});
