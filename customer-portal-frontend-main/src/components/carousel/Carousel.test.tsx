'use client'

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import Carousel from "./Carousel";

// Mock image import
jest.mock("../../assets/images/Arrow-Right-lightBlue.svg", () => "arrow.svg");

const mockCards = [
  <div key="1" data-testid="card">Card 1</div>,
  <div key="2" data-testid="card">Card 2</div>,
  <div key="3" data-testid="card">Card 3</div>,
];

describe("Carousel component", () => {
  beforeEach(() => {
    // Mock scroll container dimensions
    Object.defineProperties(HTMLElement.prototype, {
      scrollWidth: {
        configurable: true,
        get: () => 1000,
      },
      offsetWidth: {
        configurable: true,
        get: () => 300,
      },
      scrollLeft: {
        configurable: true,
        get: () => 0,
        set: () => {},
      },
    });
  });

  test("renders cards", () => {
    render(<Carousel cards={mockCards} />);
    const cards = screen.getAllByTestId("card");
    expect(cards).toHaveLength(3);
  });

  test("renders right arrow when cards > 1", () => {
    render(<Carousel cards={mockCards} />);
    const rightArrow = screen.getByRole("button", { name: "" });
    expect(rightArrow).toBeInTheDocument();
  });

  test("calls scroll function when right arrow is clicked", () => {
    render(<Carousel cards={mockCards} />);
    const container = document.querySelector(".tabs-wrapper");
    container.scrollTo = jest.fn();

    const rightArrow = screen.getByRole("button", { name: "" });
    fireEvent.click(rightArrow);

    expect(container.scrollTo).toHaveBeenCalled();
  });

  test("renders nothing when cards array is empty", () => {
    render(<Carousel cards={[]} />);
    expect(screen.queryByTestId("card")).not.toBeInTheDocument();
  });

  test("left arrow is not visible at initial scroll position", () => {
    render(<Carousel cards={mockCards} />);
    const leftArrow = document.querySelector(".left-arrow");
    expect(leftArrow).toHaveClass("hide");
  });

  test("right arrow is hidden if content fits container", () => {
    Object.defineProperty(HTMLElement.prototype, "scrollWidth", {
      configurable: true,
      get: () => 300, // Same as offsetWidth
    });

    render(<Carousel cards={mockCards} />);
    const rightArrow = document.querySelector(".right-arrow");
    expect(rightArrow).toHaveClass("hide");
  });

  test("renders gracefully if cards prop is undefined", () => {
    render(<Carousel />);
    expect(screen.queryByTestId("card")).not.toBeInTheDocument();
  });

  test("calls scroll function when left arrow is clicked", () => {
    render(<Carousel cards={mockCards} />);
    const container = document.querySelector(".tabs-wrapper");
    container.scrollTo = jest.fn();

    const leftArrow = document.querySelector(".left-arrow");
    fireEvent.click(leftArrow);

    expect(container.scrollTo).toHaveBeenCalled();
  });

  test("arrow buttons have role 'button'", () => {
    render(<Carousel cards={mockCards} />);
    const buttons = screen.getAllByRole("button");
    expect(buttons.length).toBeGreaterThanOrEqual(1);
  });
});
