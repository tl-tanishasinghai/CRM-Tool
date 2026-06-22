'use client'

import { render, screen } from "@testing-library/react";
import DocumentList from "./DocumentList";
import React from "react";
import axios from "axios";

jest.mock("axios");

const mockLoanInfo = {
  loanAccountNumber: "123456",
  loanApplicationId: "app789",
//   status: "CLOSED",
};

const mockDocumentList = [
  { id: "doc1", tagValue: "Docket" },
  { id: "doc2", tagValue: "KFS" },
];

describe("DocumentList Component", () => {
  it("renders the Documents heading", () => {
    render(<DocumentList loanInfo={{}} documentList={[]} />);
    expect(screen.getByText("Documents")).toBeInTheDocument();
  });

  it("renders correctly when documentList is empty", () => {
    render(
      <DocumentList
        loanInfo={{ loanAccountNumber: "0000", loanApplicationId: "xyz", status: "CLOSED" }}
        documentList={[]}
      />
    );
  
    expect(screen.getByText("NOC")).toBeInTheDocument();
    expect(screen.getByText("SOA")).toBeInTheDocument();
  });

  it("renders default and dynamic documents", () => {
    render(<DocumentList loanInfo={mockLoanInfo} documentList={mockDocumentList} />);
  
    expect(screen.getByText("NOC")).toBeInTheDocument();
    expect(screen.getByText("SOA")).toBeInTheDocument();
    expect(screen.getByText("Loan Agreement")).toBeInTheDocument(); // Docket becomes Loan Agreement
    expect(screen.getByText("KFS")).toBeInTheDocument();
  });

  it("NOC card has 'disabled-doc' class and is not clickable", () => {
    render(<DocumentList loanInfo={{
        loanAccountNumber: "123456",
        loanApplicationId: "app789",
      }} documentList={[]} />);
  
  // Get the element containing text 'NOC'
  const nocLabel = screen.getByText("NOC");

  // Get closest parent with class 'document-card'
  const nocCard = nocLabel.closest(".document-card");

  // Expect the NOC card to have 'disabled-doc' class
  expect(nocCard).toHaveClass("disabled-doc");
  });
  
});

