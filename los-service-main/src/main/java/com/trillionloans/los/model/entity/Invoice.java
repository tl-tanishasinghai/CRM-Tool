package com.trillionloans.los.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.trillionloans.los.util.R2dbcJsonJacksonConfig;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Builder
@Table("invoices")
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
  @Id private Long id;
  private String partnerId;
  private String anchorId;

  private BigDecimal amount;
  private String invoiceNumber;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate invoiceDate;

  @JsonSerialize(using = R2dbcJsonJacksonConfig.Serializer.class)
  @JsonDeserialize(using = R2dbcJsonJacksonConfig.Deserializer.class)
  private Json metadata;

  private String hashKey;
}
