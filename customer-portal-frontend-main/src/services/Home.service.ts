import axios, { AxiosRequestConfig } from "axios";
import { decryptData } from "../utils/customerPortalDecoder";

// Function to fetch API data
type FetchResponse = unknown[];

const fetchApiData = (
  url: string,
  authHeader?: AxiosRequestConfig
): Promise<FetchResponse> => {
  return new Promise((resolve, reject) => {
    axios
      .get(url, authHeader)
      .then(async (response) => {
        // Log only non-sensitive metadata
        console.log("API request successful");
        const decryptedData = await decryptData(response.data);
        if (!decryptedData) {
          resolve([]);
          return;
        }
        resolve(loanListParser(JSON.parse(decryptedData)));
      })
      .catch((error) => {
        // Log only non-sensitive error details
        console.error("API request failed:", error.message);
        reject(error);
      });
  });
};

// Function to parse loan list
type LoanRecord = Record<string, unknown> & {
  lendingServiceProvider?: string;
  netDisbursementAmount?: string | number;
  disbursementDate?: string;
};

const loanListParser = (apiData: LoanRecord[] = []) => {
  return apiData.map((loan) => {
    return {
      ...loan,
      company: loan.lendingServiceProvider,
      amount: `₹${loan.netDisbursementAmount ?? ""}`,
      date: loan.disbursementDate,
    };
  });
};

// Function to fetch transactions
export const fetchTransactions = (
  url: string,
  authHeader?: AxiosRequestConfig
): Promise<unknown> => {
  return new Promise((resolve, reject) => {
    axios
      .get(url, authHeader)
      .then((response) => {
        // Log only non-sensitive metadata
        console.log("Transaction fetch successful");
        resolve(transactionParser(response.data));
      })
      .catch((error) => {
        // Log only non-sensitive error details
        console.error("Transaction fetch failed:", error.message);
        reject(error);
      });
  });
};

// Function to parse transactions
const transactionParser = (transactionLists: unknown) => {
  return transactionLists;
};

export default fetchApiData;
