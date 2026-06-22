package com.trillionloans.los.config;

import com.trillionloans.los.service.S3Service;
import com.trillionloans.los.util.PKCS7Signer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class NsdlCertificateConfig {

  private final String certBucket;
  private final String certKey;
  private final String certPassword;
  private final S3Service s3Service;

  public NsdlCertificateConfig(
      S3Service s3Service,
      @Value("${nsdl.cert.bucketName}") String certBucket,
      @Value("${nsdl.cert.key}") String certKey,
      @Value("${nsdl.cert.password}") String certPassword) {
    this.s3Service = s3Service;
    this.certBucket = certBucket;
    this.certKey = certKey;
    this.certPassword = certPassword;
  }

  @Bean
  public PKCS7Signer pkcs7Signer() {
    log.info("[OPV_CERT] Starting PKCS#7 signer initialization");

    try {
      byte[] pfxBytes = s3Service.downloadFile(certBucket, certKey);
      if (Objects.isNull(pfxBytes) || pfxBytes.length == 0) {
        log.error("PFX certificate empty/null in S3.");
        return PKCS7Signer.disabled("PFX certificate empty/null in S3.");
      }

      KeyStore ks = KeyStore.getInstance("PKCS12");
      try (InputStream is = new ByteArrayInputStream(pfxBytes)) {
        ks.load(is, certPassword.toCharArray());
      }

      var aliases = ks.aliases().asIterator();
      if (!aliases.hasNext()) {
        log.error("[OPV_CERT] Keystore loaded but NO aliases found. Signer cannot be initialized.");
        return PKCS7Signer.disabled("No alias in keystore");
      }

      String alias = aliases.next();
      if (!ks.isKeyEntry(alias)) {
        log.error(
            "[OPV_CERT] Alias does not contain a private key entry. Signer cannot be initialized.");
        return PKCS7Signer.disabled("Alias without private key: " + alias);
      }

      PrivateKey privateKey = (PrivateKey) ks.getKey(alias, certPassword.toCharArray());
      X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

      if (privateKey == null || cert == null) {
        log.error(
            "[OPV_CERT] Missing PrivateKey or X509Certificate for alias in keystore. Signer cannot"
                + " be initialized.");
        return PKCS7Signer.disabled("Key/Cert missing at alias: " + alias);
      }

      log.info("[OPV_CERT] Successfully initialized PKCS7 signer from keystore.");
      return new PKCS7Signer(privateKey, cert);

    } catch (Exception e) {
      log.error(
          "[OPV_CERT] Failed to initialize signer from S3. PAN validation disabled." + " Reason={}",
          e.getMessage());
      return PKCS7Signer.disabled("Initialization error: " + e.getMessage());
    }
  }
}
