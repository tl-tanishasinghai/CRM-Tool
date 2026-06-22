'use client'

import { render, screen } from "@testing-library/react";
import Table from "./Table";
import React from "react";

describe("Table Component", () => {
  it("displays 'No Transactions found' when transactionsList is empty", () => {
    render(<Table transactionsList={[]} columns={[]} />);
    expect(screen.getByText("No Transactions found")).toBeInTheDocument();
  });

  it("renders column headers", () => {
    const columns = [
      { key: "date", label: "Date" },
      { key: "amount", label: "Amount" },
    ];

    render(
      <Table transactionsList={[{ date: "2024-01-01", amount: 100 }]} columns={columns} />
    );

    expect(screen.getByText("Date")).toBeInTheDocument();
    expect(screen.getByText("Amount")).toBeInTheDocument();
  });

  it("renders correct number of rows with transaction data", () => {
    const columns = [
      { key: "date", label: "Date" },
      { key: "amount", label: "Amount" },
    ];

    const data = [
      { date: "2024-01-01", amount: 100 },
      { date: "2024-01-02", amount: 200 },
    ];

    render(<Table transactionsList={data} columns={columns} />);

    expect(screen.getByText("2024-01-01")).toBeInTheDocument();
    expect(screen.getByText("100")).toBeInTheDocument();
    expect(screen.getByText("2024-01-02")).toBeInTheDocument();
    expect(screen.getByText("200")).toBeInTheDocument();

    // Check that two rows are rendered
    const rows = screen.getAllByRole("row");
    // rows include header + data rows
    expect(rows.length).toBe(1 + data.length);
  });

  it("renders default value when data key is missing", () => {
    const columns = [
      { key: "amount", label: "Amount", defaultValue: "N/A" },
    ];

    const data = [{}];

    render(<Table transactionsList={data} columns={columns} />);

    expect(screen.getByText("N/A")).toBeInTheDocument();
  });

  it("renders table element when data is available", () => {
    const columns = [{ key: "amount", label: "Amount" }];
    const data = [{ amount: 123 }];

    render(<Table transactionsList={data} columns={columns} />);
    expect(screen.getByRole("table")).toBeInTheDocument();
  });
});
