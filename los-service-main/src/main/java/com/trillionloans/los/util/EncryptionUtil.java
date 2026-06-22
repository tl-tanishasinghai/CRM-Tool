package com.trillionloans.los.util;

import com.trillionloans.los.exception.PiiDataConverterException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
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
      throw new PiiDataConverterException("exception while encrypting data", ex);
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
      throw new PiiDataConverterException("exception while decrypting data", ex);
    }
  }

  public String encryptForCache(String value) {
    try {
      byte[] iv = new byte[12];
      new SecureRandom().nextBytes(iv);

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      GCMParameterSpec spec = new GCMParameterSpec(128, iv);
      SecretKey key = new SecretKeySpec(keyString.getBytes(StandardCharsets.UTF_8), algorithm);
      cipher.init(Cipher.ENCRYPT_MODE, key, spec);

      byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

      byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
      System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

      return Base64.getEncoder().encodeToString(encryptedWithIv);

    } catch (Exception ex) {
      throw new PiiDataConverterException("exception while encrypting cache data", ex);
    }
  }

  public String decryptFromCache(String encryptedValue) {
    try {
      byte[] decoded = Base64.getDecoder().decode(encryptedValue);

      byte[] iv = Arrays.copyOfRange(decoded, 0, 12);
      byte[] ciphertext = Arrays.copyOfRange(decoded, 12, decoded.length);

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      GCMParameterSpec spec = new GCMParameterSpec(128, iv);
      SecretKey key = new SecretKeySpec(keyString.getBytes(StandardCharsets.UTF_8), algorithm);
      cipher.init(Cipher.DECRYPT_MODE, key, spec);

      byte[] decrypted = cipher.doFinal(ciphertext);
      return new String(decrypted, StandardCharsets.UTF_8);

    } catch (Exception ex) {
      throw new PiiDataConverterException("exception while decrypting cache data", ex);
    }
  }
}
