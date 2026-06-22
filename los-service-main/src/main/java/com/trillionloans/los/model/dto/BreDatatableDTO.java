package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "BRE Datatable DTO")
public class BreDatatableDTO {
  private String request;
  private String response;
  private String lead;
  private String scienapticstatus;
  private String amount;
  private String tenure;
  private String roi;
  private String locale;
  private String dateformat;
  private String reasons;
  private String data;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private BigDecimal orig_req_amt;

  public BreDatatableDTO(BreDatatableDTO breDatatableDTO) {
    this.request = breDatatableDTO.request;
    this.response = breDatatableDTO.response;
    this.lead = breDatatableDTO.lead;
    this.scienapticstatus = breDatatableDTO.scienapticstatus;
    this.locale = breDatatableDTO.locale;
    this.dateformat = breDatatableDTO.dateformat;
    this.amount = breDatatableDTO.amount;
    this.tenure = breDatatableDTO.tenure;
    this.roi = breDatatableDTO.roi;
    this.reasons = breDatatableDTO.reasons;
    this.data = breDatatableDTO.data;
    this.orig_req_amt = breDatatableDTO.orig_req_amt;
  }
}
