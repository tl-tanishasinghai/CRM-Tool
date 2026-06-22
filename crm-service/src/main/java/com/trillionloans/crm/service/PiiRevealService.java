package com.trillionloans.crm.service;

import com.trillionloans.crm.model.CrmModels.CustomerProfile;
import com.trillionloans.crm.model.WrapperModels.PiiField;
import com.trillionloans.crm.model.WrapperModels.PiiRevealAudit;
import com.trillionloans.crm.model.WrapperModels.PiiRevealResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PiiRevealService {

  private final ExternalDataService externalDataService;
  private final CrmStore store;

  public PiiRevealService(ExternalDataService externalDataService, CrmStore store) {
    this.externalDataService = externalDataService;
    this.store = store;
  }

  public PiiRevealResponse reveal(String leadId, String agentId, PiiField field, String reason) {
    CustomerProfile profile = externalDataService.getCustomerProfile(leadId);
    String value =
        switch (field) {
          case MOBILE -> profile.mobileNo();
          case EMAIL -> profile.email();
          case ADDRESS -> profile.address();
        };

    if (value == null || value.isBlank() || "—".equals(value)) {
      throw new NotFoundException("Requested field is not available for this customer");
    }

    PiiRevealAudit audit =
        new PiiRevealAudit(
            "reveal-" + UUID.randomUUID(),
            leadId,
            agentId,
            field,
            reason == null || reason.isBlank() ? "Agent desk unmask" : reason.trim(),
            Instant.now());
    store.addPiiRevealAudit(audit);

    return new PiiRevealResponse(field, value, audit.id(), audit.createdAt());
  }
}
