package com.trillionloans.lms.repository;

import com.google.gson.Gson;
import com.trillionloans.lms.api.strapi.StrapiApiClient;
import com.trillionloans.lms.model.dto.internal.PartnerConfigRow;
import com.trillionloans.lms.model.dto.internal.ProductControl;
import com.trillionloans.lms.util.StrapiProductControlMapper;
import io.r2dbc.postgresql.codec.Json;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class PartnerConfigViewService {

  private final StrapiApiClient strapiApiClient;
  private final StrapiProductControlMapper strapiMapper;
  private final Gson gson;

  public Flux<PartnerConfigRow> findActivePartnerProductConfigs() {
    return strapiApiClient
        .findAllChargesConfigs()
        .flatMapMany(
            configs ->
                Flux.fromIterable(configs)
                    .map(
                        chargesConfigDto -> {
                          ProductControl productControl =
                              strapiMapper.toProductControlFromChargesOnly(chargesConfigDto);
                          Json productJson =
                              Json.of(gson.toJson(productControl).getBytes(StandardCharsets.UTF_8));
                          return new PartnerConfigRow(
                              null,
                              chargesConfigDto.getPartnerCode(),
                              chargesConfigDto.getProductCode(),
                              "A",
                              productJson);
                        }));
  }
}
