package com.trillionloans.los.service.disbursal;

import static com.trillionloans.los.constant.StringConstants.RULE_NOT_FOUND;

import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.internal.RuleDTO;
import com.trillionloans.los.model.entity.RuleEntity;
import com.trillionloans.los.repository.DynamicRuleRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicRuleService {
  private final DynamicRuleRepository ruleRepository;

  public Flux<RuleEntity> loadActiveRules() {
    return ruleRepository.findByActiveTrueOrderByPriorityAsc();
  }

  public Mono<RuleEntity> createRule(RuleDTO ruleDTO) {
    validateRule(ruleDTO);
    RuleEntity entity = new RuleEntity();
    BeanUtils.copyProperties(ruleDTO, entity);
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());

    return ruleRepository.save(entity);
  }

  public Mono<RuleEntity> updateRule(Long id, RuleDTO ruleDTO) {
    return ruleRepository
        .findById(id)
        .switchIfEmpty(
            Mono.error(new BaseException(RULE_NOT_FOUND, RULE_NOT_FOUND, HttpStatus.NOT_FOUND)))
        .flatMap(
            existingRule -> {
              existingRule.setId(id);
              existingRule.setName(
                  null != ruleDTO.getName() ? ruleDTO.getName() : existingRule.getName());
              existingRule.setPriority(
                  null != ruleDTO.getPriority()
                      ? ruleDTO.getPriority()
                      : existingRule.getPriority());
              existingRule.setProductCode(
                  null != ruleDTO.getProductCode()
                      ? ruleDTO.getProductCode()
                      : existingRule.getProductCode());
              existingRule.setDescription(
                  null != ruleDTO.getDescription()
                      ? ruleDTO.getDescription()
                      : existingRule.getDescription());
              existingRule.setActive(
                  null != ruleDTO.getActive() ? ruleDTO.getActive() : existingRule.isActive());
              existingRule.setAction(
                  null != ruleDTO.getAction() ? ruleDTO.getAction() : existingRule.getAction());
              existingRule.setCondition(
                  null != ruleDTO.getCondition()
                      ? ruleDTO.getCondition()
                      : existingRule.getCondition());
              existingRule.setUpdatedAt(LocalDateTime.now());
              BeanUtils.copyProperties(existingRule, ruleDTO);
              validateRule(ruleDTO);
              return ruleRepository.save(existingRule);
            });
  }

  public Mono<RuleEntity> getRule(Long id) {
    return ruleRepository
        .findById(id)
        .switchIfEmpty(
            Mono.error(new BaseException(RULE_NOT_FOUND, RULE_NOT_FOUND, HttpStatus.NOT_FOUND)));
  }

  public Flux<RuleEntity> getAllRules() {
    return ruleRepository.findAll();
  }

  public Mono<Void> deleteRule(Long id) {
    return ruleRepository
        .findById(id)
        .switchIfEmpty(
            Mono.error(new BaseException(RULE_NOT_FOUND, RULE_NOT_FOUND, HttpStatus.NOT_FOUND)))
        .flatMap(rule -> ruleRepository.deleteById(id));
  }

  private void validateRule(RuleDTO ruleDTO) {
    try {

      MVEL.compileExpression(ruleDTO.getCondition());
      MVEL.compileExpression(ruleDTO.getAction());
    } catch (Exception e) {
      throw new BaseException("invalid mvel expression", e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }
}
