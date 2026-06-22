package com.trillionloans.los.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import lombok.Getter;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.*;
import org.bouncycastle.operator.jcajce.*;

/**
 * Utility class to create a PKCS#7 detached signature for a given payload.
 *
 * <p>PKCS#7 (aka CMS – Cryptographic Message Syntax) is used to sign or encrypt messages. Here, we
 * use it to sign JSON payloads for NSDL PAN verification APIs.
 *
 * <p>Support two modes: - Active Signer (valid private key + certificate loaded) - Disabled Signer
 * (graceful fallback if cert is missing, feature disabled, or init failed)
 */
public class PKCS7Signer {

  private final PrivateKey privateKey;
  private final X509Certificate certificate;

  @Getter private final boolean enabled;
  @Getter private final String reasonDisabled;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  // Active signer
  public PKCS7Signer(PrivateKey privateKey, X509Certificate certificate) {
    this.privateKey = privateKey;
    this.certificate = certificate;
    this.enabled = true;
    this.reasonDisabled = null;
  }

  // Disabled signer
  private PKCS7Signer(String reasonDisabled) {
    this.privateKey = null;
    this.certificate = null;
    this.enabled = false;
    this.reasonDisabled = reasonDisabled;
  }

  public static PKCS7Signer disabled(String reason) {
    return new PKCS7Signer(reason);
  }

  public String sign(String jsonPayload)
      throws CMSException, CertificateEncodingException, IOException, OperatorCreationException {
    byte[] data = jsonPayload.getBytes(StandardCharsets.UTF_8);

    CMSTypedData cmsData = new CMSProcessableByteArray(data);

    CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
    JcaContentSignerBuilder builder =
        new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC");
    ContentSigner contentSigner = builder.build(privateKey);

    gen.addSignerInfoGenerator(
        new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
            .build(contentSigner, certificate));

    gen.addCertificates(new JcaCertStore(Collections.singletonList(certificate)));

    CMSSignedData signedData = gen.generate(cmsData, false); // detached = false

    /**
     * Detached vs Attached: - detached = true → only the signature is included, not the data -
     * detached = false → both the data and the signature are included
     *
     * <p>For NSDL PAN verification APIs, they expect a detached signature (payload is sent
     * separately, signature is sent in header).
     */
    return Base64.getEncoder().encodeToString(signedData.getEncoded());
  }
}
