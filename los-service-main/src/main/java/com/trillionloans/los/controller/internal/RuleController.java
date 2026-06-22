package com.trillionloans.los.controller.internal;

import static com.trillionloans.los.constant.StringConstants.RULE_DELETE_SUCCESS;

import com.trillionloans.los.model.dto.internal.RuleDTO;
import com.trillionloans.los.model.entity.RuleEntity;
import com.trillionloans.los.service.disbursal.DynamicRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller for managing dynamic rules. Provides endpoints for creating, updating, retrieving, and
 * deleting rules.
 */
@RestController
@RequestMapping("/internal/rules")
@Slf4j
@RequiredArgsConstructor
public class RuleController {

  private final DynamicRuleService dynamicRuleService;

  /**
   * Creates a new rule.
   *
   * @param ruleDTO The data transfer object containing rule details.
   * @return A Mono wrapping a ResponseEntity with the created RuleEntity.
   */
  @PostMapping
  public Mono<ResponseEntity<Mono<RuleEntity>>> createRule(@Valid @RequestBody RuleDTO ruleDTO) {
    return Mono.just(
        ResponseEntity.status(HttpStatus.CREATED).body(dynamicRuleService.createRule(ruleDTO)));
  }

  /**
   * Updates an existing rule by ID.
   *
   * @param id The ID of the rule to update.
   * @param ruleDTO The updated rule details.
   * @return A Mono wrapping a ResponseEntity with the updated RuleEntity.
   */
  @PutMapping("/{id}")
  public Mono<ResponseEntity<Mono<RuleEntity>>> updateRule(
      @Valid @PathVariable Long id, @RequestBody RuleDTO ruleDTO) {
    return Mono.just(ResponseEntity.ok(dynamicRuleService.updateRule(id, ruleDTO)));
  }

  /**
   * Retrieves a specific rule by ID.
   *
   * @param id The ID of the rule to retrieve.
   * @return A Mono wrapping a ResponseEntity with the RuleEntity.
   */
  @GetMapping("/{id}")
  public Mono<ResponseEntity<Mono<RuleEntity>>> getRule(@Valid @PathVariable Long id) {
    return Mono.just(ResponseEntity.ok(dynamicRuleService.getRule(id)));
  }

  /**
   * Retrieves all rules.
   *
   * @return A Mono wrapping a ResponseEntity containing a Flux of RuleEntity objects.
   */
  @GetMapping
  public Mono<ResponseEntity<Flux<RuleEntity>>> getAllRules() {
    Flux<RuleEntity> rules = dynamicRuleService.getAllRules();
    return Mono.just(ResponseEntity.ok(rules));
  }

  /**
   * Deletes a specific rule by ID.
   *
   * @param id The ID of the rule to delete.
   * @return A Mono wrapping a ResponseEntity containing a success message.
   */
  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Mono<String>>> deleteRule(@Valid @PathVariable Long id) {
    return Mono.just(
        ResponseEntity.ok(dynamicRuleService.deleteRule(id).then(Mono.just(RULE_DELETE_SUCCESS))));
  }
}
