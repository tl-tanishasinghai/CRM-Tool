package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.DISBURSEMENT_CONFIG;

import com.trillionloans.los.model.dto.internal.RuleEvaluationResultDTO;
import com.trillionloans.los.repository.DynamicRuleRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service class for executing custom rules based on given facts, type, and product code. It
 * evaluates conditions and performs actions using MVEL expressions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomRuleEngineService {
  private final DynamicRuleRepository ruleRepository;

  /**
   * Executes a series of rules based on the provided facts, type, and product code. For each rule,
   * the condition is evaluated, and if met, the corresponding action is executed.
   *
   * @param facts A map of facts used for rule evaluation.
   * @param type The type of rule to be evaluated.
   * @param productCode The product code for the specific rule set.
   * @return A Mono containing the RuleEvaluationResultDTO with the modified facts.
   */
  public Mono<RuleEvaluationResultDTO> executeRules(
      Map<String, Object> facts, String type, String productCode) {
    return Mono.just(new RuleEvaluationResultDTO())
        .flatMap(
            result ->
                ruleRepository
                    .findByActiveTrueAndTypeAndProductCodeOrderByPriorityAsc(type, productCode)
                    .collectList()
                    .flatMap(
                        rules -> {
                          for (var rule : rules) {
                            boolean conditionMet = evaluateCondition(rule.getCondition(), facts);
                            if (conditionMet) {
                              evaluateAction(rule.getAction(), facts);
                            }
                          }
                          Map<String, Object> modifiedFacts = new HashMap<>(facts);
                          result.setResults(modifiedFacts);
                          return Mono.just(result);
                        }))
        .subscribeOn(Schedulers.boundedElastic());
  }

  /**
   * Evaluates a given condition using MVEL expressions and the provided facts.
   *
   * @param condition The MVEL condition expression to evaluate.
   * @param facts A map of facts used in the evaluation.
   * @return A boolean indicating whether the condition is met or not.
   */
  private boolean evaluateCondition(String condition, Map<String, Object> facts) {
    try {
      return (Boolean) MVEL.eval(condition, facts);
    } catch (Exception e) {
      log.error("[{}] error evaluating condition: {}", DISBURSEMENT_CONFIG, condition, e);
      return false;
    }
  }

  /**
   * Evaluates an action using MVEL expressions and modifies the facts accordingly.
   *
   * @param action The MVEL action expression to evaluate.
   * @param facts A map of facts to be modified by the action.
   */
  private void evaluateAction(String action, Map<String, Object> facts) {
    try {
      MVEL.eval(action, facts);
    } catch (Exception e) {
      log.error("[{}] error evaluating action: {}", DISBURSEMENT_CONFIG, action, e);
    }
  }
}
