package com.trillionloans.lms.controller.internal;

import com.trillionloans.lms.service.FacadeService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/partners/api/v1")
@RestController
@Hidden
public class FacadeController {
  FacadeService facadeService;

  @Autowired
  public FacadeController(FacadeService facadeService) {
    this.facadeService = facadeService;
  }
}
