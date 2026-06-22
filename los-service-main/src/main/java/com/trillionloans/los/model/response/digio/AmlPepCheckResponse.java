package com.trillionloans.los.model.response.digio;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AmlPepCheckResponse {
  private String risk_level;
  private boolean is_alert_generated;
  private boolean found;
  private int total_alerts;
  private int low_severity_alerts;
  private int high_severity_alerts;
  private int medium_severity_alerts;
  private List<AlertDetail> low_alert_details;
  private List<AlertDetail> high_alert_details;
  private List<AlertDetail> medium_alert_details;

  @Getter
  @Setter
  public static class AlertDetail {
    private String severity;
    private String name;
    private String date_of_birth;
    private String place_of_birth;
    private String gender;
    private String nationality;
    private String source_url;
    private double fuzzy_match_score;
    private String source_unique_id;
    private String source_name;
  }
}
