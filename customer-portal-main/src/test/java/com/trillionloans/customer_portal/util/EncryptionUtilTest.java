package com.trillionloans.customer_portal.util;

import static com.trillionloans.customer_portal.util.EncryptionUtil.getKeyFromString;
import static org.junit.jupiter.api.Assertions.*;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

public class EncryptionUtilTest {

  public static final String BASE64_SECRET_KEY = "q1N7R5b5c9R2IcQjVwtGGw==";

  public static final SecretKey SECRET_KEY = getKeyFromString(BASE64_SECRET_KEY);

  @Test
  public void testEncryptDecrypt()
      throws InvalidAlgorithmParameterException,
          NoSuchPaddingException,
          IllegalBlockSizeException,
          NoSuchAlgorithmException,
          BadPaddingException,
          InvalidKeyException {
    // Test input string
    String inputData = "Test data";

    // Encrypt the data using the secret key
    String encryptedData = EncryptionUtil.encrypt(inputData, SECRET_KEY);
    assertNotNull(encryptedData, "Encrypted data should not be null");

    // Decrypt the data using the same key
    String decryptedData = EncryptionUtil.decrypt(encryptedData, SECRET_KEY);
    assertEquals(inputData, decryptedData, "Decrypted data should match the original input data");
  }

  @Test
  public void testEncryptDecryptEmptyString()
      throws InvalidAlgorithmParameterException,
          NoSuchPaddingException,
          IllegalBlockSizeException,
          NoSuchAlgorithmException,
          BadPaddingException,
          InvalidKeyException {
    // Test empty string
    String inputData = "";
    String encryptedData = EncryptionUtil.encrypt(inputData, SECRET_KEY);
    assertNotNull(encryptedData, "Encrypted data for empty string should not be null");

    // Decrypt the encrypted empty data
    String decryptedData = EncryptionUtil.decrypt(encryptedData, SECRET_KEY);
    assertEquals(
        inputData,
        decryptedData,
        "Decrypted empty data should match the original empty input data");
  }

  @Test
  public void testEncryptDecryptSpecialCharacters()
      throws InvalidAlgorithmParameterException,
          NoSuchPaddingException,
          IllegalBlockSizeException,
          NoSuchAlgorithmException,
          BadPaddingException,
          InvalidKeyException {
    // Test special characters input string
    String inputData = "!@#$%^&*()_+";

    // Encrypt the data
    String encryptedData = EncryptionUtil.encrypt(inputData, SECRET_KEY);
    assertNotNull(encryptedData, "Encrypted data should not be null");

    // Decrypt the data
    String decryptedData = EncryptionUtil.decrypt(encryptedData, SECRET_KEY);
    assertEquals(
        inputData, decryptedData, "Decrypted special characters should match the original input");
  }

  @Test
  public void testGetKeyFromString() {
    // Verify that the key can be correctly converted from the Base64-encoded string
    SecretKey keyFromString = getKeyFromString(BASE64_SECRET_KEY);
    assertNotNull(keyFromString, "SecretKey should not be null when decoded from Base64");
  }

  @Test
  public void testGetStringFromKey() {
    // Convert the secret key to a string and verify that the Base64 encoding matches
    String keyString = EncryptionUtil.getStringFromKey(SECRET_KEY);
    assertNotNull(keyString, "Base64-encoded string from SecretKey should not be null");

    // Verify that the key string matches the original BASE64_SECRET_KEY
    assertEquals(
        BASE64_SECRET_KEY,
        keyString,
        "The Base64 string from the key should match the original secret key");
  }

  @Test
  public void testDecryptWithIncorrectKey()
      throws InvalidAlgorithmParameterException,
          NoSuchPaddingException,
          IllegalBlockSizeException,
          NoSuchAlgorithmException,
          BadPaddingException,
          InvalidKeyException {
    // Test decrypting with an incorrect key
    String inputData = "Test data";

    // Encrypt data with the correct key
    String encryptedData = EncryptionUtil.encrypt(inputData, SECRET_KEY);

    // Create an incorrect key
    SecretKey incorrectKey = getKeyFromString("IncorrectBase64SecretKey1234");

    // Attempt to decrypt with the incorrect key and expect an exception
    assertThrows(
        Exception.class,
        () -> EncryptionUtil.decrypt(encryptedData, incorrectKey),
        "Decrypting with an incorrect key should throw an exception");
  }

  @Test
  public void testEncryptDecryptPerformance()
      throws InvalidAlgorithmParameterException,
          NoSuchPaddingException,
          IllegalBlockSizeException,
          NoSuchAlgorithmException,
          BadPaddingException,
          InvalidKeyException {
    // Test for performance with larger data
    StringBuilder largeData = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      largeData.append("Test data ");
    }

    // Encrypt and decrypt large data
    String encryptedData = EncryptionUtil.encrypt(largeData.toString(), SECRET_KEY);
    assertNotNull(encryptedData, "Encrypted data should not be null");

    String decryptedData = EncryptionUtil.decrypt(encryptedData, SECRET_KEY);
    assertEquals(
        largeData.toString(),
        decryptedData,
        "Decrypted large data should match the original input");
  }

  @Test
  public void decryptData()
      throws InvalidAlgorithmParameterException,
          NoSuchPaddingException,
          IllegalBlockSizeException,
          NoSuchAlgorithmException,
          BadPaddingException,
          InvalidKeyException {

    String encryptedData =
        "4edSh0DSBVwdbWjFWWwx6YuvaJVDo0I3wTdDZjh24tUEHmCzDv0l3s3zHZzxlEXwCQOGqv6n/dVaJmfC96NktcoSQS4Lnlyx1cn9n4vYRxDc7Ogg/DcPcrXokYO0oGXYlx3erCzDK6JcGZ84UrfCAfxmUT+NRU/Fq17W1TiCF2bLI//8m6lsh31fQZYgQ6kR3lx7E6GO1adTz+HcDz7eJba9Aa4QgoxgY49VH3EizpnXto2mkigjHJ5Tdikaf/DDr8Dco3zd5b6Bht4UYLkfQNhbqGHV0xPuVFdjQYqsiuRH7+x+GAaKuHw4G3he3OAfy9PBg2RXcr7cADw20s0ynlzsvWnsp2xNMGG2HGbnGhiOG+5WGd5b2WEwVA==";
    String decryptedData = EncryptionUtil.decrypt(encryptedData, SECRET_KEY);
    assertNotNull(decryptedData);
  }
}
