package com.trillionloans.customer_portal.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {

  private static final String ALGORITHM = "AES";
  private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_SIZE = 12;
  private static final int KEY_SIZE = 256; // 256-bit AES key
  private static final int GCM_TAG_LENGTH = 16; // GCM tag length (128 bits)
  private static final int TAG_BIT_LENGTH = 128;

  // Encrypt the data
  public static String encrypt(String data, SecretKey key)
      throws NoSuchPaddingException,
          NoSuchAlgorithmException,
          IllegalBlockSizeException,
          BadPaddingException,
          InvalidAlgorithmParameterException,
          InvalidKeyException {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

    // Generate a random 12-byte IV (GCM recommends 12 bytes for IV)
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);

    GCMParameterSpec spec =
        new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv); // 128-bit authentication tag

    cipher.init(Cipher.ENCRYPT_MODE, key, spec);

    byte[] encryptedData = cipher.doFinal(data.getBytes());

    // Concatenate IV and encrypted data (IV is needed for decryption)
    byte[] ivAndEncryptedData = new byte[iv.length + encryptedData.length];
    System.arraycopy(iv, 0, ivAndEncryptedData, 0, iv.length);
    System.arraycopy(encryptedData, 0, ivAndEncryptedData, iv.length, encryptedData.length);

    // Encode as Base64 string to make it suitable for storage or transmission
    return Base64.getEncoder().encodeToString(ivAndEncryptedData);
  }

  public static String decryptToken(String encryptedData, SecretKey key)
      throws NoSuchPaddingException,
          NoSuchAlgorithmException,
          InvalidKeyException,
          IllegalBlockSizeException,
          BadPaddingException, InvalidAlgorithmParameterException {
    // Decode Base64
    byte[] ivAndEncrypted = Base64.getDecoder().decode(encryptedData);

    // Extract IV
    byte[] iv = new byte[IV_SIZE];
    System.arraycopy(ivAndEncrypted, 0, iv, 0, IV_SIZE);

    // Extract encrypted bytes
    int encLength = ivAndEncrypted.length - IV_SIZE;
    byte[] encryptedBytes = new byte[encLength];
    System.arraycopy(ivAndEncrypted, IV_SIZE, encryptedBytes, 0, encLength);

    // Init cipher
    Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
    GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
    cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

    // Decrypt
    byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
    return new String(decryptedBytes, StandardCharsets.UTF_8);
  }

  public static String decrypt(String encryptedData, SecretKey key)
      throws NoSuchPaddingException,
          NoSuchAlgorithmException,
          InvalidAlgorithmParameterException,
          InvalidKeyException,
          IllegalBlockSizeException,
          BadPaddingException {

    byte[] ivAndEncryptedData = Base64.getDecoder().decode(encryptedData);

    // Extract the IV (first 12 bytes)
    byte[] iv = new byte[12];
    System.arraycopy(ivAndEncryptedData, 0, iv, 0, iv.length);

    // Extract the encrypted data (remaining bytes)
    byte[] encryptedBytes = new byte[ivAndEncryptedData.length - iv.length];
    System.arraycopy(ivAndEncryptedData, iv.length, encryptedBytes, 0, encryptedBytes.length);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec spec =
        new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv); // 128-bit authentication tag

    cipher.init(Cipher.DECRYPT_MODE, key, spec);

    byte[] decryptedData = cipher.doFinal(encryptedBytes);

    return new String(decryptedData, StandardCharsets.UTF_8);
  }

  // Convert a string key to SecretKey
  public static SecretKey getKeyFromString(String keyStr) {
    byte[] decodedKey = Base64.getDecoder().decode(keyStr);
    return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
  }

  // Convert a SecretKey to string
  public static String getStringFromKey(SecretKey key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

  public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
    keyGen.init(KEY_SIZE); // Set the key size to 256 bits
    return keyGen.generateKey();
  }
}
