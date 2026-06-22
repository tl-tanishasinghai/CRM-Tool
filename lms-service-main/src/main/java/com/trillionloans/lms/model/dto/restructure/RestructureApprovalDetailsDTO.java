package com.trillionloans.lms.model.dto.restructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for M2P restructure approval details API response.
 *
 * @author Pawan Kumar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestructureApprovalDetailsDTO {

  private StatusEnumDTO statusEnum;
  private TimelineDTO timeline;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class StatusEnumDTO {
    private String value;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TimelineDTO {
    /** Array format: [year, month, day] */
    private List<Integer> approvedOnDate;
  }
}
