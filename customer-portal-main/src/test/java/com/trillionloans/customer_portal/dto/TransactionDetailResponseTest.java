package com.trillionloans.customer_portal.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trillionloans.customer_portal.model.dto.TransactionDetailResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TransactionDetailResponseTest {

  private TransactionDetailResponse transactionDetailResponse;

  @BeforeEach
  void setUp() {
    // Setup a default instance of TransactionDetailResponse for reuse in tests
    transactionDetailResponse = new TransactionDetailResponse();
  }

  @Test
  void testTransactionDetailResponse() {
    // Given: Create mock data for the TransactionDetailResponse object
    TransactionDetailResponse.Status status = new TransactionDetailResponse.Status();
    status.setId(1);
    status.setCode("200");
    status.setValue("Success");

    TransactionDetailResponse.Transaction transaction = new TransactionDetailResponse.Transaction();
    transaction.setId(123L);
    transaction.setAmount(1000.00);
    transaction.setExternalId("EXTERNAL123");

    TransactionDetailResponse.PaymentDetailData paymentDetailData =
        new TransactionDetailResponse.PaymentDetailData();
    paymentDetailData.setId(1L);
    paymentDetailData.setReceiptNumber("REC123");
    transaction.setPaymentDetailData(paymentDetailData);

    TransactionDetailResponse.TxnValueDateStatus txnValueDateStatus =
        new TransactionDetailResponse.TxnValueDateStatus();
    txnValueDateStatus.setValue("2022-02-12");
    transaction.setTxnValueDateStatus(txnValueDateStatus);

    transactionDetailResponse.setStatus(status);
    transactionDetailResponse.setClientId("CLIENT123");
    transactionDetailResponse.setTransactions(List.of(transaction));

    // When: Retrieve the fields
    TransactionDetailResponse.Status actualStatus = transactionDetailResponse.getStatus();
    String actualClientId = transactionDetailResponse.getClientId();
    List<TransactionDetailResponse.Transaction> transactions =
        transactionDetailResponse.getTransactions();

    // Then: Assert that the values are correctly set
    assertNotNull(actualStatus);
    assertEquals(1, actualStatus.getId());
    assertEquals("200", actualStatus.getCode());
    assertEquals("Success", actualStatus.getValue());

    assertEquals("CLIENT123", actualClientId);

    assertNotNull(transactions);
    assertEquals(1, transactions.size());

    TransactionDetailResponse.Transaction actualTransaction = transactions.get(0);
    assertEquals(123L, actualTransaction.getId());
    assertEquals(1000.00, actualTransaction.getAmount());
    assertEquals("EXTERNAL123", actualTransaction.getExternalId());
    assertNotNull(actualTransaction.getPaymentDetailData());
    assertEquals(1L, actualTransaction.getPaymentDetailData().getId());
    assertEquals("REC123", actualTransaction.getPaymentDetailData().getReceiptNumber());
    assertNotNull(actualTransaction.getTxnValueDateStatus());
    assertEquals("2022-02-12", actualTransaction.getTxnValueDateStatus().getValue());
  }

  @Test
  void testTransactionWithoutPaymentDetailData() {
    // Given: Create a Transaction without PaymentDetailData
    TransactionDetailResponse.Transaction transactionWithoutPaymentDetailData =
        new TransactionDetailResponse.Transaction();
    transactionWithoutPaymentDetailData.setId(124L);
    transactionWithoutPaymentDetailData.setAmount(500.00);
    transactionWithoutPaymentDetailData.setExternalId("EXTERNAL124");

    // When: Set the transaction in the response
    transactionDetailResponse.setTransactions(List.of(transactionWithoutPaymentDetailData));

    // Then: Ensure the PaymentDetailData is null
    assertNotNull(transactionDetailResponse.getTransactions());
    assertEquals(1, transactionDetailResponse.getTransactions().size());
    assertNull(transactionDetailResponse.getTransactions().get(0).getPaymentDetailData());
  }

  @Test
  void testTransactionDetailResponseWithNullStatus() {
    // Given: Set the Status as null
    transactionDetailResponse.setStatus(null);

    // When: Check the status field
    TransactionDetailResponse.Status actualStatus = transactionDetailResponse.getStatus();

    // Then: Assert that the status is null
    assertNull(actualStatus);
  }

  @Test
  void testTransactionDetailResponseWithEmptyTransactions() {
    // Given: Set an empty list of transactions
    transactionDetailResponse.setTransactions(List.of());

    // When: Retrieve the list of transactions
    List<TransactionDetailResponse.Transaction> transactions =
        transactionDetailResponse.getTransactions();

    // Then: Assert that the list is empty
    assertNotNull(transactions);
    assertTrue(transactions.isEmpty());
  }
}
