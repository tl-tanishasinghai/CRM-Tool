package com.trillionloans.lms.util;

import com.trillionloans.lms.exception.CryptoConverterException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EncryptionUtil {

  @Value("${encryption.algorithm}")
  private String algorithm;

  @Value("${encryption.key}")
  private String keyString;

  public String encrypt(String value) {
    try {
      SecretKey key = new SecretKeySpec(this.keyString.getBytes(StandardCharsets.UTF_8), algorithm);
      Cipher cipher = Cipher.getInstance(this.algorithm);
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encryptedBytes);
    } catch (Exception ex) {
      throw new CryptoConverterException("exception while encrypting data", ex);
    }
  }

  public String decrypt(String encryptedValue) {
    try {
      SecretKey key = new SecretKeySpec(this.keyString.getBytes(StandardCharsets.UTF_8), algorithm);
      Cipher cipher = Cipher.getInstance(this.algorithm);
      cipher.init(Cipher.DECRYPT_MODE, key);
      byte[] decodedBytes = Base64.getDecoder().decode(encryptedValue);
      byte[] decryptedBytes = cipher.doFinal(decodedBytes);
      return new String(decryptedBytes, StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new CryptoConverterException("exception while decrypting data", ex);
    }
  }
}
