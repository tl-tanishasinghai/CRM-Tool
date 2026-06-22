package com.trillionloans.los.model.request;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientValidationFunnelStatus {

  private String clientId;
  private FinalStatus finalStatus;
  private List<ValidationStep> steps = new ArrayList<>();

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidationStep {
    private StepName stepName;
    private Vendor vendor;
    private String response;
    private StepStatus status;
    private ServiceStatus serviceStatus;
  }

  public enum StepName {
    PAN_VALIDATION,
    NAME_SIMILARITY,
    IN_HOUSE_DOB_WATERFALL,
    PAN_AADHAR_LINKAGE,

    // fallback
    NULL;
  }

  public enum Vendor {
    NSDL,
    KARZA,
    IN_HOUSE_DOB_WATERFALL,
    SKIPPED,
    // fallback
    NULL;
  }

  public enum StepStatus {
    INIT,
    PASS,
    REJECT,
    SOFT_REJECT,
    SKIPPED,
    // fallback
    NULL;
  }

  public enum FinalStatus {
    INIT,
    PENDING,
    PASS,
    REJECT,
    SKIPPED,
    MANUAL_REVIEW,
    // fallback
    NULL;
  }

  public enum ServiceStatus {
    INIT,
    SUCCESS,
    FAILURE,
    SKIPPED,

    // fallback
    NULL;
  }
}
