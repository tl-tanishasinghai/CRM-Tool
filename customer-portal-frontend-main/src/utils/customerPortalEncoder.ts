import CryptoJS from 'crypto-js';

const secretKey = 'q1N7R5b5c9R2IcQjVwtGGw=='; // Store securely, not in frontend

export const encryptWithAES = (text: string): string => {
  return CryptoJS.AES.encrypt(text, secretKey).toString();
};


export const decryptWithAES = (cipherText: string): string | null => {
    try {
      const bytes = CryptoJS.AES.decrypt(cipherText, secretKey);
      const decryptedText = bytes.toString(CryptoJS.enc.Utf8);
      return decryptedText || null;
    } catch (error: any) {
      console.error('Decryption failed:', error);
      return null;
    }
  };



