package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceData {

  private RawInvoiceData rawData;
  private BulkDocumentsUploadRequest document;

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RawInvoiceData {
    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;

    private String invoiceNumber;
    private String distributorId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private BigDecimal outstanding;
    private String status;
    private String gst;
    private Map<String, String> supplementryData;

    private String identityKey;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Document {
    private String storageType;
    private String data;
  }
}
