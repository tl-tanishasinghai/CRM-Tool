package com.trillionloans.los.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {Util.class})
@ExtendWith(SpringExtension.class)
@DisabledInAotMode
class UtilTest {
  @MockBean private Environment environment;

  @Autowired private Util util;

  /** Method under test: {@link Util#getPropertiesByPrefix(String)} */
  @Test
  void testGetPropertiesByPrefix() {
    assertTrue(util.getPropertiesByPrefix("Prefix").isEmpty());
  }
}
