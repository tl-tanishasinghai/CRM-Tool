'use client'

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import FreshDeskModal from "../freshdesk/FreshDeskModal";
import axios from "axios";
import Loader from "../loader/Loader";
import { decryptData } from "../../utils/customerPortalDecoder";
import { decryptWithAES } from "../../utils/customerPortalEncoder";

interface EmbeddedModalPageProps {
  onLogout: () => void;
  setShowNavbar: (value: boolean) => void;
  setFooter: (value: boolean) => void;
}

export default function EmbeddedModalPage({
  onLogout,
  setShowNavbar,
  setFooter,
}: EmbeddedModalPageProps) {
  const router = useRouter();
  const baseUrl = process.env.NEXT_PUBLIC_CUSTOMER_PORTAL_ENDPOINT || "";
  const [customerDetails, setCustomerDetails] = useState<any>({});
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const authHeader = {
    withCredentials: true,
  };

  const handleLogout = () => {
    router.push("/login");
    onLogout();
  };

  const getUserData = async () => {
    try {
      const phoneNumber = localStorage.getItem("phoneNumber");
      if (!phoneNumber) {
        handleLogout();
        return;
      }
      setIsLoading(true);
      const customerLeadId = decryptWithAES(localStorage.getItem("GlobalLeadId") ?? "");
      const customerInfo = await axios.get(
        `${baseUrl}/customer/${customerLeadId}`,
        authHeader
      );
      const decryptedCustomerInfo = await decryptData(customerInfo?.data);
      setCustomerDetails(decryptedCustomerInfo ? JSON.parse(decryptedCustomerInfo) : {});
    } catch (error) {
      console.error("Error fetching customer data:", error);
      handleLogout();
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    setShowNavbar(false);
    setFooter(false);
    getUserData();
  }, [setShowNavbar, setFooter]);

  return (
    <div
      style={{
        minHeight: "100vh",
        padding: "24px",
        boxSizing: "border-box",
        backgroundColor: "#F5F5F5",
        overflowY: "auto",
        display: "flex",
        justifyContent: "center",
        alignItems: "flex-start",
      }}
    >
      <div style={{ width: "100%", maxWidth: "640px" }}>
        {isLoading ? (
          <div className="loader-container">
            <Loader />
          </div>
        ) : (
          <FreshDeskModal customerDetails={customerDetails} />
        )}
      </div>
    </div>
  );
}
