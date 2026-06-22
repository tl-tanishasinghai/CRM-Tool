package com.trillionloans.los.service;

import com.trillionloans.los.constant.DocumentTag;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Registry mapping document tags to their eligibility validators. Built from all
 * TagEligibilityValidator beans. Used to validate loan eligibility before Digio e-sign flow.
 */
@Component
@Slf4j
public class TagEligibilityValidatorRegistry {

  private final List<TagEligibilityValidator> validators;
  private Map<DocumentTag, TagEligibilityValidator> validatorByTag;

  public TagEligibilityValidatorRegistry(List<TagEligibilityValidator> validators) {
    this.validators = validators != null ? validators : List.of();
  }

  @PostConstruct
  void init() {
    this.validatorByTag =
        validators.stream()
            .collect(
                Collectors.toMap(TagEligibilityValidator::getSupportedTag, Function.identity()));
    log.info(
        "[TAG_ELIGIBILITY_REGISTRY] Registered validators for tags: {}", validatorByTag.keySet());
  }

  /** Returns the validator for the given tag, if one exists. */
  public Optional<TagEligibilityValidator> getValidator(DocumentTag tag) {
    return Optional.ofNullable(validatorByTag.get(tag));
  }

  /** Returns true if the tag has an associated eligibility validator (i.e. is Digio-eligible). */
  public boolean isDigioEligibleTag(DocumentTag tag) {
    return validatorByTag.containsKey(tag);
  }
}
