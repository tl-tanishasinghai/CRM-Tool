package com.trillionloans.lms.service;

import static com.trillionloans.lms.constant.StringConstants.CHARGES_LOGGER;
import static java.time.LocalDateTime.now;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.lms.api.m2p.M2PApi;
import com.trillionloans.lms.exception.NotFoundException;
import com.trillionloans.lms.model.ChargeRule;
import com.trillionloans.lms.model.ChargeRuleMapper;
import com.trillionloans.lms.model.DailyRateSlab;
import com.trillionloans.lms.model.dto.EmiReportRow;
import com.trillionloans.lms.model.dto.internal.ChargeAmounts;
import com.trillionloans.lms.model.dto.internal.PartnerConfigRow;
import com.trillionloans.lms.model.dto.internal.ProductControl;
import com.trillionloans.lms.model.entity.ChargeAuditLogEntity;
import com.trillionloans.lms.model.entity.ChargeRunLogEntity;
import com.trillionloans.lms.model.request.SaveChargesRequest;
import com.trillionloans.lms.model.response.ChargeDetailsDTO;
import com.trillionloans.lms.model.response.M2pChargeDetailsDTO;
import com.trillionloans.lms.repository.ChargeAuditLogHistoryRepository;
import com.trillionloans.lms.repository.ChargeAuditLogRepository;
import com.trillionloans.lms.repository.ChargeRunLogRepository;
import com.trillionloans.lms.repository.PartnerConfigViewService;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Service
public class ChargeService {

  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
  private static final String STANDARD_DATE_FORMAT = "dd-MM-yyyy";
  private static final String FAILED = "FAILED";
  private static final DateTimeFormatter API_DATE_FMT =
      DateTimeFormatter.ofPattern(STANDARD_DATE_FORMAT);

  private final PartnerConfigViewService partnerConfigViewService;
  private final ObjectMapper mapper;
  private final M2PApi m2PApiClient;
  private final ChargeRunLogRepository runLogRepo;
  private final ChargeAuditLogRepository auditRepo;
  private final ChargeAuditLogHistoryRepository historyRepo;

  @Value("${lms.fetch.page-limit:20000}")
  private int pageLimit;

  public Mono<Void> startChargesRun(String runDateOpt) {
    LocalDate runDate =
        (runDateOpt == null || runDateOpt.isBlank())
            ? LocalDate.now(IST)
            : LocalDate.parse(runDateOpt);
    String runDateStr = runDate.toString();

    AtomicLong emiProcessed = new AtomicLong(0);
    AtomicLong chargesAttempted = new AtomicLong(0);
    AtomicLong posted = new AtomicLong(0);
    AtomicLong skipped = new AtomicLong(0);
    AtomicLong failed = new AtomicLong(0);
    AtomicLong postingDisabled = new AtomicLong(0);

    return runLogRepo
        .save(ChargeRunLogBuilders.started(runDate))
        .flatMapMany(
            run -> {
              Long runId = run.getId();
              log.info(
                  "[{}] started charges run: {} run id: {}", CHARGES_LOGGER, runDateStr, runId);
              return partnerConfigViewService
                  .findActivePartnerProductConfigs()
                  .collectList()
                  .flatMapMany(
                      rows -> {
                        Map<String, ProductControl> productControls =
                            rows.stream()
                                .collect(
                                    Collectors.toMap(
                                        PartnerConfigRow::productCode,
                                        r -> parseProductControl(r.product_json())));

                        // 2) Build rules per product (respecting flag_to_enable_charges)
                        Map<String, List<ChargeRule>> rulesByProduct =
                            productControls.entrySet().stream()
                                .map(
                                    e -> Map.entry(e.getKey(), ChargeRuleMapper.from(e.getValue())))
                                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        if (rulesByProduct.isEmpty()) {
                          log.info(
                              "[{}] no active charge rules found run date: {}",
                              CHARGES_LOGGER,
                              runDateStr);
                          return Flux.empty();
                        }

                        // 3) Derive active_from_date per product
                        Map<String, LocalDate> activeFromByProduct =
                            productControls.entrySet().stream()
                                .map(
                                    e -> {
                                      ProductControl.ChargesConfig cfg =
                                          e.getValue().getChargesConfig();
                                      if (cfg == null
                                          || cfg.getActiveFromDate() == null
                                          || cfg.getActiveFromDate().isBlank()) {
                                        return null;
                                      }
                                      try {
                                        LocalDate dt =
                                            LocalDate.parse(cfg.getActiveFromDate().trim());
                                        return Map.entry(e.getKey(), dt);
                                      } catch (Exception ex) {
                                        log.info(
                                            "[{}] invalid active_from_date '{}' for product {}",
                                            CHARGES_LOGGER,
                                            cfg.getActiveFromDate(),
                                            e.getKey());
                                        return null;
                                      }
                                    })
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        // 4) First phase: fetch EMIs per product (snapshot), then combine
                        return Flux.fromIterable(rulesByProduct.keySet())
                            .flatMap(
                                prod -> {
                                  LocalDate activeFrom = activeFromByProduct.get(prod);
                                  return fetchOverdueEmisForProduct(prod, runDate, activeFrom)
                                      .collectList()
                                      .map(list -> Map.entry(prod, list));
                                })
                            .collectList()
                            .flatMapMany(
                                entries -> {
                                  Map<String, List<EmiReportRow>> emisByProduct =
                                      entries.stream()
                                          .collect(
                                              Collectors.toMap(
                                                  Map.Entry::getKey, Map.Entry::getValue));

                                  long totalEmis =
                                      emisByProduct.values().stream().mapToLong(List::size).sum();
                                  emiProcessed.addAndGet(totalEmis);

                                  // 5) Second phase: apply charges on the fixed snapshot
                                  return Flux.fromIterable(rulesByProduct.keySet())
                                      .concatMap(
                                          prod ->
                                              applyRulesForProduct(
                                                  prod,
                                                  rulesByProduct.get(prod),
                                                  emisByProduct.getOrDefault(prod, List.of()),
                                                  runDate,
                                                  chargesAttempted,
                                                  posted,
                                                  skipped,
                                                  postingDisabled,
                                                  failed,
                                                  runId));
                                });
                      })
                  .then(
                      runLogRepo
                          .findById(runId)
                          .flatMap(
                              rl -> {
                                rl.setCompletedAt(now());
                                rl.setStatus("COMPLETED");
                                rl.setTotalEmisProcessed(emiProcessed.get());
                                rl.setTotalChargesAttempted(chargesAttempted.get());
                                rl.setTotalPosted(posted.get());
                                rl.setTotalSkipped(skipped.get());
                                rl.setTotalPostingDisabled(postingDisabled.get());
                                rl.setTotalFailed(failed.get());
                                return runLogRepo.save(rl);
                              }))
                  .doOnSuccess(
                      x ->
                          log.info(
                              "[{}] run completed {}, run id: {}, posted: {}, skipped: {}, posting"
                                  + " disabled: {}, failed: {}, emi: {}, attempts: {}",
                              CHARGES_LOGGER,
                              runDateStr,
                              runId,
                              posted.get(),
                              skipped.get(),
                              postingDisabled.get(),
                              failed.get(),
                              emiProcessed.get(),
                              chargesAttempted.get()))
                  .doOnError(
                      e ->
                          runLogRepo
                              .findById(runId)
                              .flatMap(
                                  rl -> {
                                    rl.setCompletedAt(now());
                                    rl.setStatus(FAILED);
                                    rl.setErrorMessage(e.getMessage());
                                    return runLogRepo.save(rl);
                                  })
                              .subscribe(
                                  r ->
                                      log.error(
                                          "[{}] run failed: {}, run id: {}, error: {}",
                                          CHARGES_LOGGER,
                                          runDateStr,
                                          runId,
                                          e.getMessage(),
                                          e)))
                  .thenMany(Flux.empty());
            })
        .then();
  }

  private Mono<Void> applyRulesForProduct(
      String prod,
      List<ChargeRule> rules,
      List<EmiReportRow> emis,
      LocalDate runDate,
      AtomicLong chargesAttempted,
      AtomicLong posted,
      AtomicLong skipped,
      AtomicLong postingDisabled,
      AtomicLong failed,
      Long runId) {

    if (rules == null || rules.isEmpty()) {
      return Mono.empty();
    }

    log.info("[{}] processing product {} emis={}", CHARGES_LOGGER, prod, emis.size());

    List<ChargeRule> slabbed =
        rules.stream()
            .filter(r -> r.dailyRates() != null && !r.dailyRates().isEmpty())
            .sorted(Comparator.comparing(ChargeRule::shortCode))
            .toList();

    List<ChargeRule> dpdEquals =
        rules.stream()
            .filter(r -> r.trigger() == ChargeRule.Trigger.DPD_EQUALS)
            .sorted(Comparator.comparing(ChargeRule::shortCode))
            .toList();

    Mono<List<EmiReportRow>> emisListMono = Mono.just(emis).cache();

    Mono<Void> slabRun =
        emisListMono
            .flatMapMany(Flux::fromIterable)
            .publish(
                emisFlux ->
                    Flux.fromIterable(slabbed)
                        .concatMap(
                            rule ->
                                emisFlux
                                    .filter(
                                        emi -> {
                                          int dpd =
                                              (int) ChronoUnit.DAYS.between(emi.dueDate(), runDate);
                                          return isEligible(rule, emi.paymentStatus())
                                              && rule.dailyRates().stream()
                                                  .anyMatch(
                                                      s -> dpd >= s.dpdFrom() && dpd <= s.dpdTo());
                                        })
                                    .concatMap(
                                        emi -> {
                                          int dpd =
                                              (int) ChronoUnit.DAYS.between(emi.dueDate(), runDate);
                                          return applyCharge(
                                              runDate,
                                              rule,
                                              emi,
                                              dpd,
                                              chargesAttempted,
                                              posted,
                                              skipped,
                                              postingDisabled,
                                              failed,
                                              runId);
                                        })))
            .then();

    Flux<EmiReportRow> emiFlux = emisListMono.flatMapMany(Flux::fromIterable).cache();

    Mono<Void> dpdEqRun =
        Flux.fromIterable(dpdEquals)
            .concatMap(
                rule ->
                    emiFlux
                        .filter(
                            emi -> {
                              int dpd = (int) ChronoUnit.DAYS.between(emi.dueDate(), runDate);
                              Integer off = rule.offsetDays();
                              return isEligible(rule, emi.paymentStatus())
                                  && off != null
                                  && dpd == off;
                            })
                        .concatMap(
                            emi ->
                                applyCharge(
                                    runDate,
                                    rule,
                                    emi,
                                    (int) ChronoUnit.DAYS.between(emi.dueDate(), runDate),
                                    chargesAttempted,
                                    posted,
                                    skipped,
                                    postingDisabled,
                                    failed,
                                    runId)))
            .then();

    return slabRun.then(dpdEqRun);
  }

  private Mono<Void> applyCharge(
      LocalDate runDate,
      ChargeRule rule,
      EmiReportRow emi,
      int dpd,
      AtomicLong chargesAttempted,
      AtomicLong posted,
      AtomicLong skipped,
      AtomicLong postingDisabled,
      AtomicLong failed,
      Long runId) {
    String externalId = emi.loanId() + "-" + emi.period() + "-" + rule.shortCode() + "-" + runDate;

    ChargeAmounts amounts = computeAmounts(rule, emi, dpd);
    BigDecimal outstanding = amounts.outstanding();
    BigDecimal base = amounts.base();
    if (base == null || base.signum() <= 0) return Mono.empty();

    BigDecimal total;
    BigDecimal gstAmt;
    if (Boolean.TRUE.equals(rule.gstApplicable())) {
      BigDecimal multiplier = getGstMultiplier();
      total = base.multiply(multiplier);
      gstAmt = total.subtract(base);
      total = total.setScale(0, RoundingMode.DOWN);
    } else {
      gstAmt = BigDecimal.ZERO;
      total = base.setScale(0, RoundingMode.DOWN);
    }

    String mode = rule.getPostingDateMode();
    if (!rule.isValidPostingMode()) {
      log.info(
          "[{}] invalid posting mode: '{}' for rule: {}, default to RUN_DATE",
          CHARGES_LOGGER,
          mode,
          rule.shortCode());
      mode = "RUN_DATE";
    }
    LocalDate effectivePostedDate = "EMI_DUE_DATE".equals(mode) ? emi.dueDate() : runDate;
    BigDecimal finalTotalCharge = total;

    chargesAttempted.incrementAndGet();

    if (finalTotalCharge.signum() <= 0) {
      return skipRoundedToZero(
          externalId,
          runId,
          rule,
          emi,
          runDate,
          effectivePostedDate,
          outstanding,
          base,
          gstAmt,
          finalTotalCharge,
          skipped);
    }

    String dueDateApi = API_DATE_FMT.format(effectivePostedDate);
    SaveChargesRequest req =
        new SaveChargesRequest(
            finalTotalCharge.doubleValue(),
            rule.m2pChargeTypeId().intValue(),
            STANDARD_DATE_FORMAT,
            "en",
            dueDateApi,
            externalId);
    return auditRepo
        .existsAsTerminalDuplicate(externalId)
        .flatMap(
            exists -> {
              if (Boolean.TRUE.equals(exists)) {
                skipped.incrementAndGet();
                return auditRepo
                    .updateSkipStatus(
                        externalId, runId, "SKIPPED", "DUPLICATE", "duplicate externalId")
                    .then(
                        historyRepo
                            .insertHistory(
                                externalId,
                                runId,
                                rule.name(),
                                emi.productCode(),
                                effectivePostedDate,
                                rule.m2pChargeTypeId(),
                                outstanding,
                                base,
                                gstAmt,
                                finalTotalCharge,
                                "SKIPPED",
                                "DUPLICATE",
                                "duplicate externalId")
                            .then())
                    .onErrorResume(e -> Mono.empty());
              }

              // Respect per-charge postingEnabled flag
              if (!rule.isPostingEnabled()) {
                skipped.incrementAndGet();
                postingDisabled.incrementAndGet();
                log.info(
                    "[SAVE_CHARGES] [POSTING_DISABLED] charge details: {}", toJsonSilently(req));
                return auditRepo
                    .upsertAudit(
                        externalId,
                        runId,
                        emi.loanId(),
                        emi.period(),
                        rule.shortCode(),
                        rule.name(),
                        emi.productCode(),
                        runDate,
                        effectivePostedDate,
                        rule.m2pChargeTypeId(),
                        outstanding,
                        base,
                        gstAmt,
                        finalTotalCharge,
                        "SKIPPED",
                        "POSTING_DISABLED",
                        null)
                    .flatMap(
                        id ->
                            historyRepo.insertHistory(
                                externalId,
                                runId,
                                rule.name(),
                                emi.productCode(),
                                effectivePostedDate,
                                rule.m2pChargeTypeId(),
                                outstanding,
                                base,
                                gstAmt,
                                finalTotalCharge,
                                "SKIPPED",
                                "POSTING_DISABLED",
                                null))
                    .then();
              }

              return m2PApiClient
                  .saveCharges(String.valueOf(emi.loanId()), req)
                  .then(
                      auditRepo
                          .upsertAudit(
                              externalId,
                              runId,
                              emi.loanId(),
                              emi.period(),
                              rule.shortCode(),
                              rule.name(),
                              emi.productCode(),
                              runDate,
                              effectivePostedDate,
                              rule.m2pChargeTypeId(),
                              outstanding,
                              base,
                              gstAmt,
                              finalTotalCharge,
                              "POSTED",
                              "OK:200",
                              null)
                          .flatMap(
                              id ->
                                  historyRepo.insertHistory(
                                      externalId,
                                      runId,
                                      rule.name(),
                                      emi.productCode(),
                                      effectivePostedDate,
                                      rule.m2pChargeTypeId(),
                                      outstanding,
                                      base,
                                      gstAmt,
                                      finalTotalCharge,
                                      "POSTED",
                                      "OK:200",
                                      null))
                          .doOnSuccess(id -> posted.incrementAndGet())
                          .then())
                  .onErrorResume(
                      WebClientResponseException.class,
                      ex -> {
                        int code = ex.getStatusCode().value();
                        String payload = ex.getResponseBodyAsString();

                        boolean isDuplicate =
                            code == 403
                                && payload.contains("error.msg.loan.charge.duplicate.externalId");
                        if (isDuplicate) {
                          posted.incrementAndGet();
                          return auditRepo
                              .upsertAudit(
                                  externalId,
                                  runId,
                                  emi.loanId(),
                                  emi.period(),
                                  rule.shortCode(),
                                  rule.name(),
                                  emi.productCode(),
                                  runDate,
                                  effectivePostedDate,
                                  rule.m2pChargeTypeId(),
                                  outstanding,
                                  base,
                                  gstAmt,
                                  finalTotalCharge,
                                  "POSTED",
                                  "HTTP 403 DUPLICATE",
                                  payload)
                              .flatMap(
                                  id ->
                                      historyRepo.insertHistory(
                                          externalId,
                                          runId,
                                          rule.name(),
                                          emi.productCode(),
                                          effectivePostedDate,
                                          rule.m2pChargeTypeId(),
                                          outstanding,
                                          base,
                                          gstAmt,
                                          finalTotalCharge,
                                          "POSTED",
                                          "HTTP 403 DUPLICATE",
                                          payload))
                              .then();
                        }

                        failed.incrementAndGet();
                        return auditRepo
                            .upsertAudit(
                                externalId,
                                runId,
                                emi.loanId(),
                                emi.period(),
                                rule.shortCode(),
                                rule.name(),
                                emi.productCode(),
                                runDate,
                                effectivePostedDate,
                                rule.m2pChargeTypeId(),
                                outstanding,
                                base,
                                gstAmt,
                                finalTotalCharge,
                                FAILED,
                                "HTTP " + code,
                                payload)
                            .flatMap(
                                id ->
                                    historyRepo.insertHistory(
                                        externalId,
                                        runId,
                                        rule.name(),
                                        emi.productCode(),
                                        effectivePostedDate,
                                        rule.m2pChargeTypeId(),
                                        outstanding,
                                        base,
                                        gstAmt,
                                        finalTotalCharge,
                                        FAILED,
                                        "HTTP " + code,
                                        payload))
                            .then();
                      })
                  .onErrorResume(
                      Throwable.class,
                      ex -> {
                        failed.incrementAndGet();
                        return auditRepo
                            .upsertAudit(
                                externalId,
                                runId,
                                emi.loanId(),
                                emi.period(),
                                rule.shortCode(),
                                rule.name(),
                                emi.productCode(),
                                runDate,
                                effectivePostedDate,
                                rule.m2pChargeTypeId(),
                                outstanding,
                                base,
                                gstAmt,
                                finalTotalCharge,
                                FAILED,
                                "EX",
                                ex.getMessage())
                            .flatMap(
                                id ->
                                    historyRepo.insertHistory(
                                        externalId,
                                        runId,
                                        rule.name(),
                                        emi.productCode(),
                                        effectivePostedDate,
                                        rule.m2pChargeTypeId(),
                                        outstanding,
                                        base,
                                        gstAmt,
                                        finalTotalCharge,
                                        FAILED,
                                        "EX",
                                        ex.getMessage()))
                            .then();
                      });
            });
  }

  private Mono<Void> skipRoundedToZero(
      String externalId,
      Long runId,
      ChargeRule rule,
      EmiReportRow emi,
      LocalDate runDate,
      LocalDate effectivePostedDate,
      BigDecimal outstanding,
      BigDecimal base,
      BigDecimal gstAmt,
      BigDecimal finalTotalCharge,
      AtomicLong skipped) {
    log.info(
        "[{}] [ROUNDED_TO_ZERO] skipping M2P post for externalId={}, base={}, total={}",
        CHARGES_LOGGER,
        externalId,
        base,
        finalTotalCharge);
    return auditRepo
        .existsAsTerminalDuplicate(externalId)
        .flatMap(
            exists -> {
              if (Boolean.TRUE.equals(exists)) {
                skipped.incrementAndGet();
                return auditRepo
                    .updateSkipStatus(
                        externalId, runId, "SKIPPED", "DUPLICATE", "duplicate externalId")
                    .then(
                        historyRepo
                            .insertHistory(
                                externalId,
                                runId,
                                rule.name(),
                                emi.productCode(),
                                effectivePostedDate,
                                rule.m2pChargeTypeId(),
                                outstanding,
                                base,
                                gstAmt,
                                finalTotalCharge,
                                "SKIPPED",
                                "DUPLICATE",
                                "duplicate externalId")
                            .then())
                    .onErrorResume(e -> Mono.empty());
              }

              skipped.incrementAndGet();
              return auditRepo
                  .upsertAudit(
                      externalId,
                      runId,
                      emi.loanId(),
                      emi.period(),
                      rule.shortCode(),
                      rule.name(),
                      emi.productCode(),
                      runDate,
                      effectivePostedDate,
                      rule.m2pChargeTypeId(),
                      outstanding,
                      base,
                      gstAmt,
                      finalTotalCharge,
                      "SKIPPED",
                      "ROUNDED_TO_ZERO",
                      null)
                  .flatMap(
                      id ->
                          historyRepo.insertHistory(
                              externalId,
                              runId,
                              rule.name(),
                              emi.productCode(),
                              effectivePostedDate,
                              rule.m2pChargeTypeId(),
                              outstanding,
                              base,
                              gstAmt,
                              finalTotalCharge,
                              "SKIPPED",
                              "ROUNDED_TO_ZERO",
                              null))
                  .then();
            });
  }

  private ChargeAmounts computeAmounts(ChargeRule rule, EmiReportRow emi, int dpd) {
    if (rule.type() == ChargeRule.Type.PCT_PI_REMAINING) {
      BigDecimal piDue = nz(emi.principalDue()).add(nz(emi.interestDue()));
      BigDecimal piPaid = nz(emi.principalPaid()).add(nz(emi.interestPaid()));
      BigDecimal remaining = piDue.subtract(piPaid);
      if (remaining.signum() <= 0) {
        return new ChargeAmounts(remaining, BigDecimal.ZERO);
      }

      // case 1: daily-rate-based calculation
      if (rule.dailyRates() != null && !rule.dailyRates().isEmpty()) {
        Optional<DailyRateSlab> slabOpt =
            rule.dailyRates().stream()
                .filter(s -> dpd >= s.dpdFrom() && dpd <= s.dpdTo())
                .findFirst();
        if (slabOpt.isEmpty()) {
          return new ChargeAmounts(remaining, BigDecimal.ZERO);
        }
        BigDecimal dailyRate = slabOpt.get().dailyRate();
        BigDecimal base = remaining.multiply(dailyRate);
        return new ChargeAmounts(remaining, base);
      }

      // case 2: lump-sum (no dailyRates) – use rule.value() as percentage multiplier
      if (rule.value() != null) {
        BigDecimal base = remaining.multiply(rule.value());
        return new ChargeAmounts(remaining, base);
      }

      return new ChargeAmounts(remaining, BigDecimal.ZERO);

    } else if (rule.type() == ChargeRule.Type.FLAT) {
      BigDecimal base = nz(rule.value());
      // outstanding not meaningful for FLAT; keep null
      return new ChargeAmounts(BigDecimal.ZERO, base);
    }

    throw new UnsupportedOperationException("Unknown calculation type: " + rule.type());
  }

  private boolean isEligible(ChargeRule rule, String paymentStatus) {
    if (paymentStatus == null) return false;
    String normalized = paymentStatus.trim().toUpperCase();
    Map<String, String> map =
        Map.of(
            "NOT PAID", "NOT_PAID",
            "PARTIAL PAID", "PARTIAL",
            "FULLY PAID", "FULLY_PAID");
    String key = map.getOrDefault(normalized, normalized);
    Set<String> allowed =
        rule.paymentStatusAllowed() == null
            ? Set.of()
            : rule.paymentStatusAllowed().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    return allowed.contains(key);
  }

  private ProductControl parseProductControl(Json json) {
    try {
      return mapper.readValue(json.asString(), ProductControl.class);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid product_json", e);
    }
  }

  private BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private BigDecimal getGstMultiplier() {
    return new BigDecimal("1.18");
  }

  // Per-product EMI fetch with product-specific activeFromDate
  public Flux<EmiReportRow> fetchOverdueEmisForProduct(
      String productCode, LocalDate asOnDate, LocalDate activeFromDate) {

    int limit = pageLimit;
    return fetchOverdueEmisForProductPaged(productCode, asOnDate, activeFromDate, 0, limit);
  }

  private Flux<EmiReportRow> fetchOverdueEmisForProductPaged(
      String productCode, LocalDate asOnDate, LocalDate activeFromDate, int offset, int limit) {

    return m2PApiClient
        .fetchOverdueEmisForProduct(productCode, activeFromDate, asOnDate, offset, limit)
        .flatMapMany(
            page -> {
              if (page.isEmpty()) {
                return Flux.empty();
              }
              Flux<EmiReportRow> currentPage = Flux.fromIterable(page);
              if (page.size() < limit) {
                return currentPage;
              }
              return currentPage.concatWith(
                  fetchOverdueEmisForProductPaged(
                      productCode, asOnDate, activeFromDate, offset + limit, limit));
            });
  }

  public Mono<List<ChargeDetailsDTO>> getCharges(String loanAccountNumber) {
    Flux<M2pChargeDetailsDTO> chargeDetailsFlux =
        m2PApiClient
            .getChargeDetailsAgainstLoanAccount(loanAccountNumber)
            .filter(
                charge ->
                    charge.getChargeTimeType() != null
                        && charge.getChargeTimeType().getId() != null
                        && charge.getChargeTimeType().getId() != 1
                        && charge.getChargeTimeType().getId() != 12);
    Flux<ChargeAuditLogEntity> chargeAuditLogEntities =
        auditRepo.findByLoanId(Long.parseLong(loanAccountNumber));
    return chargeDetailsFlux
        .collectList()
        .flatMap(
            chargeDetailsList -> {
              if (chargeDetailsList.isEmpty()) {
                log.info(
                    "[{}] no charge details found for the loan account: {}",
                    "GET_CHARGES",
                    loanAccountNumber);
                return Mono.error(
                    new NotFoundException(
                        "no charge details found for loan account: " + loanAccountNumber));
              }
              return chargeAuditLogEntities
                  .collectMap(ChargeAuditLogEntity::getExternalId)
                  .flatMap(
                      chargeAuditLogMap -> {
                        List<ChargeDetailsDTO> mergedResults = new ArrayList<>();
                        chargeDetailsList.forEach(
                            chargeDetail -> {
                              ChargeAuditLogEntity dbEntity =
                                  chargeAuditLogMap.get(chargeDetail.getExternalId());
                              if (Objects.nonNull(dbEntity)) {
                                ChargeDetailsDTO dto =
                                    ChargeDetailsDTO.builder()
                                        .externalId(chargeDetail.getExternalId())
                                        .amount(chargeDetail.getAmount())
                                        .amountPaid(chargeDetail.getAmountPaid())
                                        .chargeType(dbEntity.getShortCode())
                                        .chargeLeviedTimeStamp(dbEntity.getCreatedAt().toString())
                                        .build();
                                mergedResults.add(dto);
                              } else {
                                ChargeDetailsDTO dto =
                                    ChargeDetailsDTO.builder()
                                        .externalId(chargeDetail.getExternalId())
                                        .amount(chargeDetail.getAmount())
                                        .amountPaid(chargeDetail.getAmountPaid())
                                        .build();
                                mergedResults.add(dto);
                              }
                            });
                        return Mono.just(mergedResults);
                      });
            });
  }

  static final class ChargeRunLogBuilders {
    private ChargeRunLogBuilders() {}

    static ChargeRunLogEntity started(LocalDate runDate) {
      ChargeRunLogEntity e = new ChargeRunLogEntity();
      e.setRunDate(runDate);
      e.setStartedAt(java.time.LocalDateTime.now());
      e.setStatus("STARTED");
      e.setTotalEmisProcessed(0L);
      e.setTotalChargesAttempted(0L);
      e.setTotalPosted(0L);
      e.setTotalSkipped(0L);
      e.setTotalPostingDisabled(0L);
      e.setTotalFailed(0L);
      return e;
    }
  }

  private String toJsonSilently(Object o) {
    try {
      return mapper.writeValueAsString(o);
    } catch (Exception e) {
      return String.valueOf(o);
    }
  }
}
