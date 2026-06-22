package com.trillionloans.customer_portal.util.RpsDocumentUtil;

import com.trillionloans.customer_portal.exception.BaseException;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

public class TableFormatUtil {

  public static Mono<Object> addInfoInTable(RpsTable table, List<String> headerNames) {

    for (String header : headerNames) {
      try {
        table.addCell(header, new Color(0, 150, 255));
      } catch (IOException e) {
        return Mono.error(
            new BaseException("INTERNAL SERVER ERROR", null, HttpStatus.INTERNAL_SERVER_ERROR));
      }
    }
    return Mono.empty();
  }

  public static Mono<Object> addBodyInfoInTable(RpsTable table, List<String> names) {

    for (String name : names) {
      try {
        if (name == null || name.equalsIgnoreCase("null")) {
          table.addCell("-", new Color(255, 255, 255));
        } else {
          table.addCell(name, new Color(255, 255, 255));
        }
      } catch (IOException e) {
        return Mono.error(
            new BaseException("INTERNAL SERVER ERROR", null, HttpStatus.INTERNAL_SERVER_ERROR));
      }
    }
    return Mono.empty();
  }

  public static Mono<Object> addCustInfoTable(RpsTable table, List<String> headerNames) {
    for (String header : headerNames) {
      try {
        table.addCell(header, new Color(255, 255, 255));
      } catch (IOException e) {
        return Mono.error(
            new BaseException("INTERNAL SERVER ERROR", null, HttpStatus.INTERNAL_SERVER_ERROR));
      }
    }
    return Mono.empty();
  }
}
