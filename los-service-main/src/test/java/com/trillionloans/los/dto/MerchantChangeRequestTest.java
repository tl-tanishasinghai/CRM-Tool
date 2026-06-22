package com.trillionloans.los.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.trillionloans.los.model.request.MerchantChangeRequest;
import org.junit.jupiter.api.Test;

public class MerchantChangeRequestTest {

  @Test
  void testAllArgsConstructorAndGetter() {
    MerchantChangeRequest.Associations associations = new MerchantChangeRequest.Associations();
    associations.setAnchor("ANCHOR123");

    MerchantChangeRequest request = new MerchantChangeRequest(associations);

    assertThat(request.getAssociations()).isNotNull();
    assertThat(request.getAssociations().getAnchor()).isEqualTo("ANCHOR123");
  }

  @Test
  void testNoArgsConstructorAndSetters() {
    MerchantChangeRequest request = new MerchantChangeRequest();
    MerchantChangeRequest.Associations associations = new MerchantChangeRequest.Associations();
    associations.setAnchor("ANCHOR456");

    request.setAssociations(associations);

    assertThat(request.getAssociations().getAnchor()).isEqualTo("ANCHOR456");
  }

  @Test
  void testNestedAssociationObject() {
    MerchantChangeRequest.Associations associations = new MerchantChangeRequest.Associations();
    associations.setAnchor("ANCHOR789");

    assertThat(associations.getAnchor()).isEqualTo("ANCHOR789");
  }
}
