package com.trillionloans.lms.model;

import com.trillionloans.lms.model.dto.internal.ProductControl;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ChargeRuleMapper {

  private ChargeRuleMapper() {}

  public static List<ChargeRule> from(ProductControl productControl) {
    // No product control → no rules
    if (productControl == null) {
      return List.of();
    }

    ProductControl.ChargesConfig chargesConfig = productControl.getChargesConfig();
    // No chargesConfig node → product has no charge rules
    if (chargesConfig == null) {
      return List.of();
    }

    // Master ON/OFF per product (treat null as OFF)
    if (!Boolean.TRUE.equals(chargesConfig.getFlagToEnableCharges())) {
      return List.of();
    }

    // No entries → nothing to map
    if (chargesConfig.getCharges() == null || chargesConfig.getCharges().isEmpty()) {
      return List.of();
    }

    LocalDate activeFromDate = null;
    if (chargesConfig.getActiveFromDate() != null && !chargesConfig.getActiveFromDate().isBlank()) {
      try {
        activeFromDate = LocalDate.parse(chargesConfig.getActiveFromDate().trim());
      } catch (Exception ex) {
        log.warn("Invalid active_from_date '{}' – ignoring", chargesConfig.getActiveFromDate());
      }
    }

    LocalDate finalActiveFromDate = activeFromDate;

    return chargesConfig.getCharges().stream()
        .map(entry -> toRule(entry, finalActiveFromDate))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private static ChargeRule toRule(
      ProductControl.ChargeEntry chargeEntry, LocalDate activeFromDate) {
    if (chargeEntry == null) return null;

    ChargeRule.Type type;
    try {
      type = ChargeRule.Type.valueOf(chargeEntry.getCalculationType().trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      log.info(
          "Invalid calculation_type '{}', default to PCT_PI_REMAINING",
          chargeEntry.getCalculationType());
      type = ChargeRule.Type.PCT_PI_REMAINING;
    }

    ChargeRule.Trigger triggerType = ChargeRule.Trigger.NONE;
    if (chargeEntry.getTrigger() != null) {
      String triggerStr = chargeEntry.getTrigger().trim().toUpperCase();
      if ("DPD_EQUALS".equals(triggerStr)) {
        triggerType = ChargeRule.Trigger.DPD_EQUALS;
      } else if ("MONTH_END_OVERDUE".equals(triggerStr)) {
        triggerType = ChargeRule.Trigger.MONTH_END_OVERDUE;
      }
    }

    List<DailyRateSlab> dailyRateSlabs =
        chargeEntry.getDailyRates() == null
            ? List.of()
            : chargeEntry.getDailyRates().stream()
                .map(
                    rate ->
                        new DailyRateSlab(
                            rate.getDpdFrom(),
                            rate.getDpdTo(),
                            rate.getDailyRate(),
                            rate.getDescription()))
                .collect(Collectors.toList());

    String postingDateMode =
        chargeEntry.getPostingDateMode() != null ? chargeEntry.getPostingDateMode() : "RUN_DATE";

    return ChargeRule.builder()
        .name(chargeEntry.getName())
        .shortCode(chargeEntry.getChargeShortCode())
        .type(type)
        .value(chargeEntry.getValue())
        .gstApplicable(chargeEntry.isGstApplicable())
        .m2pChargeTypeId(chargeEntry.getM2pChargeTypeId())
        .offsetDays(chargeEntry.getOffsetDays())
        .trigger(triggerType)
        .paymentStatusAllowed(chargeEntry.getPaymentStatusAllowed())
        .dailyRates(dailyRateSlabs)
        .postingDateMode(postingDateMode)
        .postingEnabled(chargeEntry.getPostingEnabled())
        .build();
  }
}
