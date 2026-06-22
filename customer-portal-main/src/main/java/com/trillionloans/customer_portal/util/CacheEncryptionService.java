package com.trillionloans.customer_portal.util;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CacheEncryptionService {

  private final boolean encryptionEnabled;
  private final SecretKey secretKey;

  public CacheEncryptionService(
      @Value("${app.encryption.enabled}") boolean encryptionEnabled,
      @Value("${app.encryption.key}") String keyStr) {
    this.encryptionEnabled = encryptionEnabled;
    this.secretKey = EncryptionUtil.getKeyFromString(keyStr);
  }

  public String encrypt(String data) {
    if (!encryptionEnabled || data == null) {
      return data;
    }
    try {
      return EncryptionUtil.encrypt(data, secretKey);
    } catch (Exception e) {
      throw new RuntimeException("Encryption failed", e);
    }
  }

  public String decrypt(String encryptedData) {
    if (!encryptionEnabled || encryptedData == null) {
      return encryptedData;
    }
    try {
      return EncryptionUtil.decrypt(encryptedData, secretKey);
    } catch (Exception e) {
      throw new RuntimeException("Decryption failed", e);
    }
  }
}
