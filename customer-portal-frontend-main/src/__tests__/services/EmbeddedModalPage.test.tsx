import React from "react";
import { render, waitFor, screen } from "@testing-library/react";
import EmbeddedModalPage from "../../components/embeddedModal/EmbeddedModalPage";
import axios from "axios";
import { decryptData } from "../../utils/customerPortalDecoder";
import { MemoryRouter } from "react-router-dom";

jest.mock("axios");
jest.mock("../../utils/customerPortalDecoder", () => ({
  decryptData: jest.fn(),
}));

jest.mock("../../components/loader/Loader", () => () => <div>Loader</div>);
jest.mock(
  "../../components/freshdesk/FreshDeskModal",
  () =>
    ({ customerDetails }) => (
      <div>FreshDeskModal Loaded with {Object.keys(customerDetails).length} fields</div>
    )
);

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockNavigate,
}));

describe("EmbeddedModalPage", () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  });

  const setShowNavbar = jest.fn();
  const setFooter = jest.fn();

  test("redirects to login if phoneNumber is missing", async () => {
    render(
      <MemoryRouter>
        <EmbeddedModalPage setShowNavbar={setShowNavbar} setFooter={setFooter} />
      </MemoryRouter>
    );

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith("/login"));
  });

  test("shows loader during fetch and renders FreshDeskModal after data loads", async () => {
    const mockPhone = "9876543210";
    const mockLeadResponse = { data: "encryptedLead" };
    const mockCustomerResponse = { data: "encryptedCustomer" };
    const decryptedLead = JSON.stringify([{ leadId: "LEAD123" }]);
    const decryptedCustomer = JSON.stringify({ name: "Test User", panNumber: "ABCDE1234F" });

    localStorage.setItem("phoneNumber", JSON.stringify(mockPhone));
    axios.get
      .mockResolvedValueOnce(mockLeadResponse)
      .mockResolvedValueOnce(mockCustomerResponse);

    decryptData
      .mockResolvedValueOnce(decryptedLead)
      .mockResolvedValueOnce(decryptedCustomer);

    render(
      <MemoryRouter>
        <EmbeddedModalPage setShowNavbar={setShowNavbar} setFooter={setFooter} />
      </MemoryRouter>
    );

    expect(screen.getByText("Loader")).toBeInTheDocument();

    await waitFor(() =>
      expect(screen.getByText(/FreshDeskModal Loaded/)).toBeInTheDocument()
    );
    expect(setShowNavbar).toHaveBeenCalledWith(false);
    expect(setFooter).toHaveBeenCalledWith(false);
  });
});
