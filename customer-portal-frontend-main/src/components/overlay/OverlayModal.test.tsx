'use client'

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import OverlayModal from "./OverlayModal";

describe("OverlayModal", () => {
  test("does not render when isOpen is false", () => {
    const { container } = render(
      <OverlayModal isOpen={false} onClose={() => {}}>
        <p>Modal content</p>
      </OverlayModal>
    );
    expect(container.firstChild).toBeNull(); // Should return null
  });

  test("renders children when isOpen is true", () => {
    render(
      <OverlayModal isOpen={true} onClose={() => {}}>
        <p>Modal content</p>
      </OverlayModal>
    );
    expect(screen.getByText("Modal content")).toBeInTheDocument();
    expect(screen.getByRole("button")).toHaveClass("overlay-close-btn");
  });

  test("calls onClose when close button is clicked", () => {
    const onCloseMock = jest.fn();

    render(
      <OverlayModal isOpen={true} onClose={onCloseMock}>
        <p>Modal content</p>
      </OverlayModal>
    );

    fireEvent.click(screen.getByRole("button"));
    expect(onCloseMock).toHaveBeenCalledTimes(1);
  });
});
