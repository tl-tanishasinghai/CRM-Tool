package com.trillionloans.los.config.annotations;

import com.trillionloans.los.constant.Event;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ControllerEvent {
  Event event();
}
