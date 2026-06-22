// base64Decoder.test block
import CryptoJS from 'crypto-js';
import { decryptData as decryptECB } from '../utils/base64Decoder'; // base64Decoder.js

// customerPortalDecoder.test block
import { decryptData as decryptGCM } from '../utils/customerPortalDecoder'; // customerPortalDecoder.js

// formatCommas.test block
import formatWithCommas from '../utils/formatCommas'; // formatCommas.js

// 🧪 Mock global crypto for GCM test
global.crypto = {
  subtle: {
    importKey: jest.fn(() => Promise.resolve('fakeKey')),
    decrypt: jest.fn(() => Promise.resolve(new TextEncoder().encode('Decrypted message')))
  }
};

describe('base64Decoder.js (ECB)', () => {
  const originalParse = CryptoJS.enc.Base64.parse;
  const originalDecrypt = CryptoJS.AES.decrypt;

  beforeEach(() => {
    CryptoJS.enc.Base64.parse = jest.fn(() => 'mockParsed');
    CryptoJS.AES.decrypt = jest.fn(() => ({
      toString: () => 'DecryptedValue'
    }));
  });

  afterEach(() => {
    CryptoJS.enc.Base64.parse = originalParse;
    CryptoJS.AES.decrypt = originalDecrypt;
  });

  test('decryptData returns decrypted string', () => {
    const result = decryptECB('mockBase64');
    expect(result).toBe('DecryptedValue');
  });

  test('decryptData returns null on error', () => {
    CryptoJS.AES.decrypt = () => ({ toString: () => '' });
    const result = decryptECB('invalidBase64');
    expect(result).toBeNull();
  });
});

describe('customerPortalDecoder.js (GCM)', () => {
  test('decryptData successfully returns decoded string', async () => {
    const base64Encoded = btoa('123456789012' + 'encryptedData'); // Mock IV + data
    const result = await decryptGCM(base64Encoded);
    expect(result).toBe('Decrypted message');
  });

  test('decryptData returns null on failure', async () => {
    crypto.subtle.decrypt.mockRejectedValueOnce(new Error('decryption failed'));
    const result = await decryptGCM('invalid');
    expect(result).toBeNull();
  });
});

describe('formatCommas.js', () => {
  test('formats valid numbers with commas', () => {
    expect(formatWithCommas(1234567, '₹')).toBe('₹1,234,567');
    expect(formatWithCommas('987654321', '$')).toBe('$987,654,321');
  });

  test('returns "-" for invalid or 0 input', () => {
    expect(formatWithCommas(null, '₹')).toBe('-');
    expect(formatWithCommas('NaN', '₹')).toBe('-');
    expect(formatWithCommas(0, '₹')).toBe('-');
  });
});
