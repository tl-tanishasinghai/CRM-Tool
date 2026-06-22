package com.trillionloans.los.model.response.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class M2pNachMandateResponseDTO {
  public Integer requestId;
  public String entityType;
  public Integer entityId;
  public List<Integer> requestedOn;
  public String status;
  public String umrn;
  public String bankAccountHolderName;
  public String bankName;
  public String branchName;
  public String bankAccountNumber;
  public String micr;
  public String ifsc;
  public AccountTypeEnum accountTypeEnum;
  public List<Integer> periodStartDate;
  public List<Integer> periodEndDate;
  public Boolean periodUntilCancelled;
  public DebitTypeEnum debitTypeEnum;
  public Double amount;
  public DebitFrequencyEnum debitFrequencyEnum;
  public String channel;
  public String mode;
  public Long createdOn;
  public Boolean isPhysicalFileUploaded;

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class AccountTypeEnum {
    public Integer id;
    public String code;
    public String value;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class DebitTypeEnum {
    public Integer id;
    public String code;
    public String value;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class DebitFrequencyEnum {
    public Integer id;
    public String code;
    public String value;
  }
}
