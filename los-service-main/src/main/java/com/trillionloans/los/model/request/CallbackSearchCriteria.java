package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the criteria parameters for searching callback logs. This class encapsulates the
 * filters that can be applied when querying callback logs.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Parameters for searching callback logs")
public class CallbackSearchCriteria {

  /** A list of product codes to filter the callback logs. */
  private List<String> productCodes;

  /** A list of callback types to filter the logs (e.g., notification, update). */
  private List<String> types;

  /** A list of unique identifiers for specific callback logs to include in the search. */
  private List<Long> ids;

  /** The start date for filtering callback logs, formatted as ISO 8601. */
  private String startDate;

  /** The end date for filtering callback logs, formatted as ISO 8601. */
  private String endDate;

  /** A list of reference IDs to filter the callback logs. */
  private List<String> referenceIds;

  /** A flag indicating whether to check for exceptions in the callback logs. */
  private Boolean exceptionCheck;
}
