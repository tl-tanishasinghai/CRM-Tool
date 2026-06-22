import axios from "axios";
import fetchApiData, { fetchTransactions } from "../../services/Home.service";
import { decryptData } from "../../utils/customerPortalDecoder";

jest.mock("axios");
jest.mock("../../utils/customerPortalDecoder", () => ({
  decryptData: jest.fn(),
}));

describe("Home.service", () => {
  describe("fetchApiData", () => {
    const mockUrl = "https://api.example.com/loans";
    const mockHeader = { headers: { Authorization: "Bearer abc123" } };
    const encryptedData = '{"encrypted":"something"}';
    const decryptedPayload = JSON.stringify([
      {
        netDisbursementAmount: 25000,
        lendingServiceProvider: "LoanCo",
        disbursementDate: "2024-05-15",
      },
    ]);

    it("should successfully fetch, decrypt, and parse loan data", async () => {
      axios.get.mockResolvedValueOnce({ data: encryptedData });
      decryptData.mockResolvedValueOnce(decryptedPayload);

      const result = await fetchApiData(mockUrl, mockHeader);

      expect(result).toEqual([
        {
          netDisbursementAmount: 25000,
          lendingServiceProvider: "LoanCo",
          disbursementDate: "2024-05-15",
          amount: "₹25000",
          company: "LoanCo",
          date: "2024-05-15",
        },
      ]);
    });

    it("should handle fetch error gracefully", async () => {
      axios.get.mockRejectedValueOnce(new Error("Network error"));
      await expect(fetchApiData(mockUrl, mockHeader)).rejects.toThrow("Network error");
    });
  });

  describe("fetchTransactions", () => {
    const mockUrl = "https://api.example.com/transactions";
    const mockHeader = { headers: { Authorization: "Bearer token123" } };
    const sampleTxns = [
      { txnId: "TXN001", amount: 100 },
      { txnId: "TXN002", amount: 200 },
    ];

    it("should fetch and return transaction data", async () => {
      axios.get.mockResolvedValueOnce({ data: sampleTxns });

      const result = await fetchTransactions(mockUrl, mockHeader);
      expect(result).toEqual(sampleTxns);
    });

    it("should handle transaction fetch failure", async () => {
      axios.get.mockRejectedValueOnce(new Error("Unauthorized"));
      await expect(fetchTransactions(mockUrl, mockHeader)).rejects.toThrow("Unauthorized");
    });
  });
});
