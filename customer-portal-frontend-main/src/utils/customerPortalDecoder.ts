const base64Key = "q1N7R5b5c9R2IcQjVwtGGw=="; // Base64 encoded AES key


// Base64 decoder function
function base64ToArrayBuffer(base64: string) {
  const binaryString = atob(base64); // Decode base64 to binary string
  const length = binaryString.length;
  const buffer = new ArrayBuffer(length);
  const view = new Uint8Array(buffer);
  for (let i = 0; i < length; i++) {
    view[i] = binaryString.charCodeAt(i);
  }
  return buffer;
}

// Function to decrypt data using AES/GCM
export async function decryptData(encryptedDataBase64: string) {
  // Convert the base64-encoded key to an ArrayBuffer
  const keyBuffer = base64ToArrayBuffer(base64Key);

  // Import the key as a CryptoKey
  const key = await crypto.subtle.importKey(
    "raw", // Raw key material
    keyBuffer, // Key material as ArrayBuffer
    { name: "AES-GCM" }, // The algorithm to use
    false, // The key is not extractable
    ["decrypt"] // The key can only be used for decryption
  );

  // Decode the base64-encoded encrypted data
  const ivAndEncryptedData = base64ToArrayBuffer(encryptedDataBase64);

  // Extract the IV (first 12 bytes)
  const iv = ivAndEncryptedData.slice(0, 12);

  // Extract the encrypted data (remaining bytes)
  const encryptedBytes = ivAndEncryptedData.slice(12);

  // Set up the GCM parameters
  const algorithm = {
    name: "AES-GCM",
    iv: iv,
    tagLength: 128, // The authentication tag length (in bits)
  };

  try {
    // Decrypt the data
    const decryptedData = await crypto.subtle.decrypt(
      algorithm, // Algorithm to use (AES-GCM)
      key, // The decryption key
      encryptedBytes // The encrypted data
    );

    // Decode the decrypted data to string (assuming UTF-8 encoding)
    const decoder = new TextDecoder("utf-8");
    return decoder.decode(decryptedData);
  } catch (err) {
    console.error("Decryption failed", err);
    return null;
  }
}
