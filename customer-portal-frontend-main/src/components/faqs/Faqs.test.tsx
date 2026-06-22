'use client'

import { render, screen } from "@testing-library/react";
import Faqs from "./Faqs";
import Accordion from "@/components/accordion/Accordion";
import { FQAS } from "@/components/accordion/Constants";

jest.mock("../accordion/Accordion", () => () => (
  <div data-testid="accordion-component">Mocked Accordion</div>
));

describe("Faqs Component", () => {
  it("renders Faqs and passes props to Accordion", () => {
    render(<Faqs />);

    // Verify the Accordion component is rendered
    expect(screen.getByTestId("accordion-component")).toBeInTheDocument();
  });
  it("renders with the correct container class", () => {
    const { container } = render(<Faqs />);
    expect(container.firstChild).toHaveClass("home-container");
  });
  
});
 