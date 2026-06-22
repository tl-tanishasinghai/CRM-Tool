'use client'

import React from "react";
import { render, fireEvent, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import FreshDeskModal from "./FreshDeskModal";
import axios from "axios";

jest.mock("axios");

describe("FreshDeskModal", () => {
  const mockCustomerDetails = {
    mobileNo: "9876543210",
    email: "user@example.com",
    panNumber: "ABCDE1234F",
    loanAccounts: ["LN123"]
  };

  beforeEach(() => {
    axios.get.mockResolvedValue({
      data: { categories: ["General", "Technical", "Billing"] }
    });
  });

  test("renders prefilled fields from customerDetails", async () => {
    render(<FreshDeskModal customerDetails={mockCustomerDetails} />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("9876543210")).toBeDisabled();
      expect(screen.getByDisplayValue("user@example.com")).toBeDisabled();
      expect(screen.getByDisplayValue("ABCDE1234F")).toBeDisabled();
      expect(screen.getByText("Raise a Concern")).toBeInTheDocument();
    });
  });

  test("shows validation errors when submitting empty form", async () => {
    render(<FreshDeskModal customerDetails={{ loanAccounts: ["LN123"] }} />);
    fireEvent.click(screen.getByText("Submit"));

    await waitFor(() => {
      expect(screen.getByText(/Enter a 10-digit mobile number/)).toBeInTheDocument();
      expect(screen.getByText(/Enter a valid email address/)).toBeInTheDocument();
      expect(screen.getByText(/Enter a valid PAN/)).toBeInTheDocument();
      expect(screen.getByText(/Description cannot be empty/)).toBeInTheDocument();
      expect(screen.getByText(/Please select a concern category/)).toBeInTheDocument();
      expect(screen.getByText(/Please select a loan ID/)).toBeInTheDocument();
    });
  });

  test("blocks file upload beyond 5 files and hides error after 2 seconds", async () => {
    jest.useFakeTimers();

    const files = Array.from({ length: 6 }, (_, i) =>
      new File(["data"], `file${i}.txt`, { type: "text/plain" })
    );

    const { container } = render(<FreshDeskModal customerDetails={mockCustomerDetails} />);
    const input = container.querySelector("input[type='file']");
    fireEvent.change(input, { target: { files } });

    expect(screen.getByText(/You can upload upto 5 attachments only/)).toBeInTheDocument();

    jest.advanceTimersByTime(2000);
    await waitFor(() =>
      expect(screen.queryByText(/You can upload upto 5 attachments only/)).not.toBeInTheDocument()
    );

    jest.useRealTimers();
  });

  test("removes a file from uploaded list", async () => {
    const file = new File(["content"], "doc.pdf", { type: "application/pdf" });

    const { container } = render(<FreshDeskModal customerDetails={mockCustomerDetails} />);
    const input = container.querySelector("input[type='file']");
    fireEvent.change(input, { target: { files: [file] } });

    expect(screen.getByText("doc.pdf")).toBeInTheDocument();

    fireEvent.click(screen.getByText("✖"));
    expect(screen.queryByText("doc.pdf")).not.toBeInTheDocument();
  });
});
