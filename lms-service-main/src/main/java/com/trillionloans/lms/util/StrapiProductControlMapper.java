package com.trillionloans.lms.util;

import com.trillionloans.lms.model.dto.internal.ProductControl;
import com.trillionloans.lms.model.dto.strapi.StrapiCallbackDto;
import com.trillionloans.lms.model.dto.strapi.StrapiChargeEntryDto;
import com.trillionloans.lms.model.dto.strapi.StrapiChargesConfigDto;
import com.trillionloans.lms.model.dto.strapi.StrapiDailyRateDto;
import com.trillionloans.lms.model.dto.strapi.StrapiProductConfigDto;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class StrapiProductControlMapper {

  public ProductControl toProductControl(
      StrapiProductConfigDto productConfigDto, StrapiChargesConfigDto chargesConfigDto) {
    List<ProductControl.Flow> flows = toFlows(productConfigDto);
    ProductControl.ChargesConfig chargesConfig = toChargesConfig(chargesConfigDto);
    return new ProductControl(flows, chargesConfig);
  }

  public ProductControl toProductControlFromChargesOnly(StrapiChargesConfigDto chargesConfigDto) {
    return new ProductControl(Collections.emptyList(), toChargesConfig(chargesConfigDto));
  }

  private List<ProductControl.Flow> toFlows(StrapiProductConfigDto dto) {
    if (dto == null || dto.getCallbacks() == null) {
      return Collections.emptyList();
    }
    return dto.getCallbacks().stream().map(this::toFlow).collect(Collectors.toList());
  }

  private ProductControl.Flow toFlow(StrapiCallbackDto cb) {
    return new ProductControl.Flow(
        cb.getIdentifier(),
        cb.getFunctionName(),
        cb.getPartnerUri(),
        cb.getCallMethod(),
        cb.getRetryCount(),
        cb.getLoggerHeader(),
        cb.isCtaCallFlag(),
        cb.getCtaName(),
        cb.getWrittenOffDpdDays(),
        cb.getConditions());
  }

  private ProductControl.ChargesConfig toChargesConfig(StrapiChargesConfigDto dto) {
    if (dto == null) {
      return null;
    }
    List<ProductControl.ChargeEntry> entries =
        dto.getCharges() == null
            ? Collections.emptyList()
            : dto.getCharges().stream().map(this::toChargeEntry).collect(Collectors.toList());
    return new ProductControl.ChargesConfig(
        dto.getFlagToEnableCharges(), dto.getActiveFromDate(), entries);
  }

  private ProductControl.ChargeEntry toChargeEntry(StrapiChargeEntryDto e) {
    List<ProductControl.ChargeEntry.DailyRate> dailyRates =
        e.getDailyRates() == null
            ? Collections.emptyList()
            : e.getDailyRates().stream().map(this::toDailyRate).collect(Collectors.toList());
    return new ProductControl.ChargeEntry(
        e.getName(),
        e.getChargeShortCode(),
        e.getCalculationType(),
        e.getValue(),
        Boolean.TRUE.equals(e.getGstApplicable()),
        e.getM2pChargeTypeId() != null ? e.getM2pChargeTypeId() : 0L,
        e.getOffsetDays(),
        e.getTrigger(),
        e.getPaymentStatusAllowed(),
        dailyRates,
        e.getFrequency(),
        e.getPostingDateMode(),
        e.getPostingEnabled());
  }

  private ProductControl.ChargeEntry.DailyRate toDailyRate(StrapiDailyRateDto d) {
    return new ProductControl.ChargeEntry.DailyRate(
        d.getDpdFrom(), d.getDpdTo(), d.getDailyRate(), d.getDescription());
  }
}
