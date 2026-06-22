package com.trillionloans.lms.controller.internal;

import com.trillionloans.lms.service.NotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@RequestMapping("/internal/notification")
@RestController
@Hidden
@AllArgsConstructor
public class NotificationController {
  private final NotificationService notificationService;

  @GetMapping("/loan-closure")
  public Mono<ResponseEntity<Mono<?>>> triggerLoanClosureNotifications() {
    return Mono.just(ResponseEntity.ok(notificationService.triggerLoanClosureNotifications()));
  }

  @GetMapping("/noc-delivery")
  public Mono<ResponseEntity<Mono<?>>> triggerNocDeliveryNotifications() {
    return Mono.just(ResponseEntity.ok(notificationService.triggerNocDeliveryNotifications()));
  }

  @GetMapping("/bulk-dpd-trigger")
  public Mono<ResponseEntity<String>> triggerBulkDPDNotifications() {
    return Mono.deferContextual(
        ctx -> {
          notificationService
              .triggerBulkDPDNotifications()
              .contextWrite(Context.of(ctx))
              .subscribe();
          return Mono.just(
              ResponseEntity.accepted().body("Bulk DPD notifications trigger request accepted."));
        });
  }

  @GetMapping("/bulk-sms-trigger")
  public Mono<ResponseEntity<List<String>>> triggerWelcomeSmsNotifications() {
    return notificationService
        .triggerWelcomeSmsNotifications()
        .collectList()
        .map(ResponseEntity::ok);
  }

  @GetMapping("/loan-agreement-sms-trigger")
  public Mono<ResponseEntity<String>> triggerLoanAgreementNotifications() {
    return Mono.deferContextual(
        ctx -> {
          notificationService
              .triggerLoanAgreementNotifications()
              .contextWrite(Context.of(ctx))
              .subscribe();
          return Mono.just(
              ResponseEntity.accepted()
                  .body("Loan agreement notifications trigger request accepted."));
        });
  }

  @GetMapping("/loan-agreement-welcome-notification-trigger")
  public Mono<ResponseEntity<String>> triggerWelcomeAndLoanAgreementNotifications() {
    return Mono.deferContextual(
        ctx -> {
          notificationService
              .processWelcomeAndLoanAgreementNotifications()
              .contextWrite(Context.of(ctx))
              .subscribe();
          return Mono.just(
              ResponseEntity.accepted()
                  .body("loan agreement and welcome notifications trigger request accepted."));
        });
  }

  @GetMapping("/ckyc-sms-trigger")
  public Mono<ResponseEntity<String>> triggerCkycNotifications() {
    return Mono.deferContextual(
        ctx -> {
          notificationService.triggerCkycNotifications().contextWrite(Context.of(ctx)).subscribe();
          return Mono.just(
              ResponseEntity.accepted().body("CKYC notifications trigger request accepted."));
        });
  }

  @GetMapping("/historical-ckyc-sms-trigger")
  public Mono<ResponseEntity<String>> triggerHistoricalCkycNotifications() {
    return Mono.deferContextual(
        ctx -> {
          notificationService
              .triggerHistoricalCkycNotifications()
              .contextWrite(Context.of(ctx))
              .subscribe();
          return Mono.just(
              ResponseEntity.accepted()
                  .body("Historical CKYC notifications trigger request accepted."));
        });
  }

  @GetMapping("/re-kyc-sms-tracker-sync")
  public Mono<ResponseEntity<String>> syncReKycSmsTracker() {
    return Mono.deferContextual(
        ctx -> {
          notificationService.syncReKycSmsTracker().contextWrite(Context.of(ctx)).subscribe();
          return Mono.just(ResponseEntity.accepted().body("Syncing of Re-Kyc Tracker accepted."));
        });
  }

  @GetMapping("/re-kyc-sms-trigger")
  public Mono<ResponseEntity<String>> triggerReKycNotifications() {
    return Mono.deferContextual(
        ctx -> {
          notificationService.triggerReKycNotifications().contextWrite(Context.of(ctx)).subscribe();
          return Mono.just(
              ResponseEntity.accepted().body("Re-Kyc notifications trigger request accepted."));
        });
  }
}
