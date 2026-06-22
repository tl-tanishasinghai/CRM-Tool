'use client'

// ToastProvider.test.js
import React from "react";
import { render, screen, act, fireEvent } from "@testing-library/react";
import { ToastProvider, useToast } from "./ToastContext";

// Helper component to trigger toast actions
const TestComponent = () => {
  const { addToast, clearToasts } = useToast();

  return (
    <>
      <button
        onClick={() => addToast("Hello World", "info", 500)}
      >
        Add Toast
      </button>
      <button
        onClick={() => addToast("Dismissible Toast", "success", 3000, true)}
      >
        Add Dismissible Toast
      </button>
      <button onClick={clearToasts}>Clear All</button>
    </>
  );
};

describe("ToastProvider", () => {
  beforeEach(() => {
    jest.useFakeTimers(); // control timers for auto-dismiss
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it("renders without crashing", () => {
    render(
      <ToastProvider>
        <TestComponent />
      </ToastProvider>
    );
    expect(screen.getByText("Add Toast")).toBeInTheDocument();
  });

  it("adds a toast and displays it", () => {
    render(
      <ToastProvider>
        <TestComponent />
      </ToastProvider>
    );

    fireEvent.click(screen.getByText("Add Toast"));
    expect(screen.getByText("Hello World")).toBeInTheDocument();
  });

  it("automatically removes toast after duration", () => {
    render(
      <ToastProvider>
        <TestComponent />
      </ToastProvider>
    );

    fireEvent.click(screen.getByText("Add Toast"));
    expect(screen.getByText("Hello World")).toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(500); // simulate time passing
    });

    expect(screen.queryByText("Hello World")).not.toBeInTheDocument();
  });

  it("allows dismissing a dismissible toast manually", () => {
    render(
      <ToastProvider>
        <TestComponent />
      </ToastProvider>
    );

    fireEvent.click(screen.getByText("Add Dismissible Toast"));
    const toastMessage = screen.getByText("Dismissible Toast");
    expect(toastMessage).toBeInTheDocument();

    const closeButton = toastMessage.parentElement.querySelector(".toast-close");
    fireEvent.click(closeButton);

    expect(screen.queryByText("Dismissible Toast")).not.toBeInTheDocument();
  });

  it("clears all toasts with clearToasts", () => {
    render(
      <ToastProvider>
        <TestComponent />
      </ToastProvider>
    );

    fireEvent.click(screen.getByText("Add Toast"));
    fireEvent.click(screen.getByText("Add Dismissible Toast"));
    expect(screen.getByText("Hello World")).toBeInTheDocument();
    expect(screen.getByText("Dismissible Toast")).toBeInTheDocument();

    fireEvent.click(screen.getByText("Clear All"));

    expect(screen.queryByText("Hello World")).not.toBeInTheDocument();
    expect(screen.queryByText("Dismissible Toast")).not.toBeInTheDocument();
  });
});
