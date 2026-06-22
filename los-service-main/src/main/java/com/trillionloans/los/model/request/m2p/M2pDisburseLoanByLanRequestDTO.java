package com.trillionloans.los.model.request.m2p;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * request body for disbursing a loan by loan account number (LAN). used for reverse feed disbursal
 * marking via /v1/loans/{lan}?command=disburse api.
 */
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Request body for disbursing loan by LAN")
public class M2pDisburseLoanByLanRequestDTO {

  @Schema(description = "Actual disbursement date", example = "2026-02-19")
  private String actualDisbursementDate;

  @Schema(description = "Date format", example = "yyyy-MM-dd")
  private String dateFormat;

  @Schema(description = "Locale", example = "en")
  private String locale;

  @Schema(description = "Payment type ID", example = "1")
  private Integer paymentTypeId;

  @Schema(description = "Note/description for the disbursement")
  private String note;

  @Schema(description = "Bank account detail ID (nullable)")
  private Integer bankAccountDetailId;

  @Schema(description = "UTR/Routing code for the disbursement")
  private String routingCode;
}
