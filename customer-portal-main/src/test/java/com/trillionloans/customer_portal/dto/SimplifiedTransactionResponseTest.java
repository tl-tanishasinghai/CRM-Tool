package com.trillionloans.customer_portal.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.trillionloans.customer_portal.model.dto.SimplifiedTransactionResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SimplifiedTransactionResponseTest {

  @Test
  void testTransactionBuilder() {
    // Given: Create a Transaction object using the builder pattern
    SimplifiedTransactionResponse.Transaction transaction =
        SimplifiedTransactionResponse.Transaction.builder()
            .amount(1000.00)
            .date("2025-02-12")
            .value("2025-02-12")
            .build();

    // Then: Assert that the values are correctly set
    assertEquals(1000.00, transaction.getAmount());
    assertEquals("2025-02-12", transaction.getDate());
    assertEquals("2025-02-12", transaction.getValue());
  }

  @Test
  void testSimplifiedTransactionResponseBuilder() {
    // Given: Create a SimplifiedTransactionResponse with a list of transactions
    SimplifiedTransactionResponse.Transaction transaction1 =
        SimplifiedTransactionResponse.Transaction.builder()
            .amount(1000.00)
            .date("2025-02-12")
            .value("2025-02-12")
            .build();

    SimplifiedTransactionResponse.Transaction transaction2 =
        SimplifiedTransactionResponse.Transaction.builder()
            .amount(2000.00)
            .date("2025-02-13")
            .value("2025-02-13")
            .build();

    SimplifiedTransactionResponse response =
        SimplifiedTransactionResponse.builder()
            .transactions(List.of(transaction1, transaction2))
            .build();

    // Then: Assert that the response contains the correct number of transactions
    assertNotNull(response);
    assertEquals(2, response.getTransactions().size());

    // And: Assert that the values of the first transaction are correct
    SimplifiedTransactionResponse.Transaction firstTransaction = response.getTransactions().get(0);
    assertEquals(1000.00, firstTransaction.getAmount());
    assertEquals("2025-02-12", firstTransaction.getDate());
    assertEquals("2025-02-12", firstTransaction.getValue());

    // And: Assert that the values of the second transaction are correct
    SimplifiedTransactionResponse.Transaction secondTransaction = response.getTransactions().get(1);
    assertEquals(2000.00, secondTransaction.getAmount());
    assertEquals("2025-02-13", secondTransaction.getDate());
    assertEquals("2025-02-13", secondTransaction.getValue());
  }

  @Test
  void testTransactionEmptyBuilder() {
    // Given: Create a Transaction object with no fields set
    SimplifiedTransactionResponse.Transaction transaction =
        SimplifiedTransactionResponse.Transaction.builder().build();

    // Then: Assert that all fields are null (since nothing is set)
    assertNull(transaction.getAmount());
    assertNull(transaction.getDate());
    assertNull(transaction.getValue());
  }
}
