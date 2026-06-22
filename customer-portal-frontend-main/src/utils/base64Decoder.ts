import CryptoJS from "crypto-js";

// Predefined Base64 secret key from your Java code

// Decode the Base64 secret key into a WordArray
const secretKey = process.env.NEXT_PUBLIC_BASE64_SECRET_KEY;
const key = secretKey ? CryptoJS.enc.Base64.parse(secretKey) : null;

// Function to decrypt the data
export const decryptData = (encryptedBase64: string) => {
  try {
    if (!key) {
      throw new Error("Missing NEXT_PUBLIC_BASE64_SECRET_KEY");
    }
    // Decode the encrypted data from Base64
    const encryptedBytes = CryptoJS.enc.Base64.parse(encryptedBase64);

    // Decrypt the data using AES and the decoded key
    const decryptedBytes = CryptoJS.AES.decrypt(
      encryptedBase64,
      key,
      { mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7 }
    );

    // Convert decrypted bytes to a UTF-8 string
    const decryptedData = decryptedBytes.toString(CryptoJS.enc.Utf8);

    if (!decryptedData) {
      throw new Error("Decryption failed");
    }

    return decryptedData;
  } catch (error) {
    console.error("Decryption error:", error);
    return null;
  }
};
