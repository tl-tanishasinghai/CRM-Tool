package com.trillionloans.los.controller.internal;

import com.trillionloans.los.model.request.CallbackSearchCriteria;
import com.trillionloans.los.service.CallbackRetryService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller for handling callback retry operations. This controller exposes an endpoint to retry
 * callbacks based on specified criteria.
 */
@RequestMapping("/internal/callbacks")
@Hidden
@AllArgsConstructor
@RestController
public class CallbackRetryController {

  private final CallbackRetryService callbackRetryService;

  /**
   * Endpoint to retry callbacks based on the provided search criteria.
   *
   * @param criteria the criteria to search for callbacks to be retried
   * @return a Mono containing a ResponseEntity with a list of retry results
   */
  @PostMapping("/retry")
  public Mono<ResponseEntity<Mono<List<String>>>> searchCallbacks(
      @RequestBody CallbackSearchCriteria criteria) {
    return Mono.just(ResponseEntity.ok(callbackRetryService.retryCallbacks(criteria)));
  }
}
