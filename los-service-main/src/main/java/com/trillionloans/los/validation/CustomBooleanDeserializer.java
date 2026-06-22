package com.trillionloans.los.validation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;

public class CustomBooleanDeserializer extends JsonDeserializer<Boolean> {
  @Override
  public Boolean deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    String value = jsonParser.getText();
    String fieldName = jsonParser.getCurrentName();
    if (value != null && !value.equals("true") && !value.equals("false")) {
      throw new InvalidFormatException(
          jsonParser, "please pass the boolean value for field " + fieldName, value, Boolean.class);
    }
    return Boolean.valueOf(value);
  }
}
