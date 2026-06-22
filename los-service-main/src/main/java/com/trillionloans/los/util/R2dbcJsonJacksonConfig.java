package com.trillionloans.los.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import io.r2dbc.postgresql.codec.Json;
import java.io.IOException;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class R2dbcJsonJacksonConfig {

  /** Serializer: Converts io.r2dbc.postgresql.codec.Json to JSON output for the API response. */
  public static class Serializer extends JsonSerializer<Json> {
    @Override
    public void serialize(Json value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      if (value == null) {
        gen.writeNull();
        return;
      }
      // value.asString() returns the raw JSON string from the DB
      ObjectMapper mapper = (ObjectMapper) gen.getCodec();
      JsonNode node = mapper.readTree(value.asString());
      gen.writeTree(node);
    }
  }

  /**
   * Deserializer: Converts incoming JSON from the RequestBody back to
   * io.r2dbc.postgresql.codec.Json.
   */
  public static class Deserializer extends JsonDeserializer<Json> {
    @Override
    public Json deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonNode node = p.getCodec().readTree(p);
      return Json.of(node.toString());
    }
  }
}
