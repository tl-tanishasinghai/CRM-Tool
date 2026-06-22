package com.trillionloans.customer_portal.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trillionloans.customer_portal.constant.ProductNameMapping;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductNameMappingTest {

  @BeforeEach
  void setup() {
    Map<String, String> mappings = new HashMap<>();

    // Only the new numeric product ID mappings
    mappings.put("1", "Merchant Loan");
    mappings.put("2", "Merchant Loan");
    mappings.put("3", "Term Loan");
    mappings.put("4", "Merchant Loan");
    mappings.put("5", "Merchant Loan");
    mappings.put("6", "Business Loan");
    mappings.put("7", "Business Loan");
    mappings.put("8", "Term Loan");
    mappings.put("9", "Term Loan");
    mappings.put("10", "Vehicle Loan");
    mappings.put("11", "Vehicle Loan");
    mappings.put("12", "Term Loan");
    mappings.put("13", "Term Loan");
    mappings.put("14", "Term Loan");
    mappings.put("15", "Business Loan");
    mappings.put("16", "Business Loan");
    mappings.put("17", "Term Loan");
    mappings.put("18", "Term Loan");
    mappings.put("19", "Vehicle Loan");
    mappings.put("20", "Term Loan");
    mappings.put("21", "Trade Finance");
    mappings.put("22", "Merchant Loan");
    mappings.put("23", "Term Loan");
    mappings.put("24", "Term Loan");
    mappings.put("25", "Personal Loan");
    mappings.put("26", "Personal Loan");
    mappings.put("27", "Personal Loan");

    ProductNameMapping.setProductNameMapping(mappings);
  }

  @Test
  void testGetProductName_ValidInputs_ReturnsProductName() {
    assertEquals("Merchant Loan", ProductNameMapping.getProductNameByProductId("1"));
    assertEquals("Term Loan", ProductNameMapping.getProductNameByProductId("3"));
    assertEquals("Vehicle Loan", ProductNameMapping.getProductNameByProductId("10"));
    assertEquals("Trade Finance", ProductNameMapping.getProductNameByProductId("21"));
    assertEquals("Personal Loan", ProductNameMapping.getProductNameByProductId("27"));
  }

  @Test
  void testGetProductName_InvalidProductCode_ReturnsEmpty() {
    assertEquals("", ProductNameMapping.getProductNameByProductId("UNKNOWN_CODE"));
  }

  @Test
  void testGetProductName_BlankInputs_ReturnsEmpty() {
    assertEquals("", ProductNameMapping.getProductNameByProductId(""));
    assertEquals("", ProductNameMapping.getProductNameByProductId(" "));
    assertEquals("", ProductNameMapping.getProductNameByProductId(null));
  }
}
