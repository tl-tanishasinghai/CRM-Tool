package com.trillionloans.los.util;

import static org.assertj.core.api.Assertions.*;

import com.trillionloans.los.exception.PiiDataConverterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EncryptionUtilTest {

  private EncryptionUtil encryptionUtil;

  @BeforeEach
  void setUp() throws Exception {
    encryptionUtil = new EncryptionUtil();

    var algorithmField = EncryptionUtil.class.getDeclaredField("algorithm");
    algorithmField.setAccessible(true);
    algorithmField.set(encryptionUtil, "AES");

    var keyField = EncryptionUtil.class.getDeclaredField("keyString");
    keyField.setAccessible(true);
    keyField.set(encryptionUtil, "1234567890123456");
  }

  @Test
  void testEncryptDecrypt_success() {
    String input = "mySecretData";

    String encrypted = encryptionUtil.encrypt(input);
    assertThat(encrypted).isNotBlank();

    String decrypted = encryptionUtil.decrypt(encrypted);
    assertThat(decrypted).isEqualTo(input);
  }

  @Test
  void testEncrypt_withInvalidAlgorithm_shouldThrow() throws Exception {
    // Invalid algorithm injection
    var algoField = EncryptionUtil.class.getDeclaredField("algorithm");
    algoField.setAccessible(true);
    algoField.set(encryptionUtil, "INVALID");

    assertThatThrownBy(() -> encryptionUtil.encrypt("data"))
        .isInstanceOf(PiiDataConverterException.class)
        .hasMessageContaining("exception while encrypting");
  }

  @Test
  void testDecrypt_withCorruptedBase64_shouldThrow() {
    assertThatThrownBy(() -> encryptionUtil.decrypt("not-a-valid-base64"))
        .isInstanceOf(PiiDataConverterException.class)
        .hasMessageContaining("exception while decrypting");
  }
}
