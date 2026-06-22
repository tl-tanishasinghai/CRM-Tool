package com.trillionloans.customer_portal.model.internal;

import com.trillionloans.customer_portal.constant.ProductOfficeMapping;
import com.trillionloans.customer_portal.exception.BaseException;
import com.trillionloans.customer_portal.model.dto.ClientDetailsCpResponseDto;
import com.trillionloans.customer_portal.model.response.RpsResponseDto.PeriodsItem;
import com.trillionloans.customer_portal.model.response.RpsResponseDto.ResponseRpsDTO;
import com.trillionloans.customer_portal.util.DateTimeUtil;
import com.trillionloans.customer_portal.util.LoanDetailsUtil;
import com.trillionloans.customer_portal.util.RpsDocumentUtil.RpsTable;
import com.trillionloans.customer_portal.util.RpsDocumentUtil.TableFormatUtil;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RpsPdfBuilder {

  private static final class Paths {
    static final String LOGO_PATH = "Images/TrillionLoans-Logo.png";
  }

  private static final class Fonts {
    static final PDFont HEADING_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    static final PDFont TABLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  }

  private static final class Labels {
    static final String HEADING = "REPAYMENT SCHEDULE";
    static final String WAIVED = "Waived Off Amount";
    static final String CUSTOMER_NAME = "Customer Name";
    static final String LOAN_AMOUNT = "Loan Amount";
    static final String LOAN_ACCOUNT_ID = "Loan Account ID";
    static final String REPAYMENT_FREQUENCY = "Repayment Frequency";
    static final String PARTNER_NAME = "Partner Name";
    static final String INTEREST_RATE = "Rate of Interest";
    static final String TENURE = "Tenure";
    static final String NUM_INSTALLMENTS = "Number of Installments";
    static final String[] RPS_HEADER_BASE = {
      "Installment Number",
      "EMI Due Date",
      "Opening Principal",
      "EMI Amount",
      "Principal Amount",
      "Interest",
      "Fees",
      "Penalties",
      "Principal Outstanding",
      "Status",
      "Outstanding",
      "Paid Date"
    };
  }

  public byte[] generatePdf(
      ResponseRpsDTO rpsResponse, ClientDetailsCpResponseDto loanDetails, String loanAccountNumber)
      throws IOException {

    try (PDDocument doc = new PDDocument();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      PDPage page = new PDPage(PDRectangle.A1);
      doc.addPage(page);

      try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {

        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();

        addLogo(doc, contentStream, pageHeight);
        addHeading(contentStream, pageWidth, pageHeight);
        String tenure = getTenure(loanAccountNumber, loanDetails);

        // Customer Info Table
        RpsTable custInfo = new RpsTable(doc, contentStream);
        List<Integer> custWidths = Arrays.asList(415, 415, 415, 415);
        custInfo.setTable(
            custWidths, 40, 12, (int) (pageHeight - 260), (int) pageHeight, (int) pageWidth);
        custInfo.setTableFont(Fonts.HEADING_FONT, 20, Color.BLACK);

        TableFormatUtil.addCustInfoTable(
            custInfo, getCustomerDetail(tenure, rpsResponse, loanDetails));

        // RPS Header Table
        List<String> headers =
            getRpsHeaderData(rpsResponse.getRepaymentSchedule().getTotalWaived());
        RpsTable rpsInfo = new RpsTable(doc, contentStream);
        List<Integer> rpsWidths =
            getRpsTotalColumnNum(rpsResponse.getRepaymentSchedule().getTotalWaived()).get(0);
        rpsInfo.setTable(
            rpsWidths, 30, 12, (int) (pageHeight - 450), (int) pageHeight, (int) pageWidth);
        rpsInfo.setTableFont(Fonts.TABLE_FONT, 14, Color.BLACK);

        TableFormatUtil.addInfoInTable(rpsInfo, headers);

        AtomicReference<Integer> n = new AtomicReference<>(0);
        for (PeriodsItem p : rpsResponse.getRepaymentSchedule().getPeriods()) {

          List<String> row =
              getRpsTableData(
                  rpsResponse.getStatus().getValue(),
                  rpsResponse,
                  rpsResponse.getRepaymentSchedule().getTotalWaived(),
                  p,
                  n,
                  rpsResponse.getRepaymentSchedule().getTotalWaived());
          n.getAndSet(n.get() + 1);
          TableFormatUtil.addBodyInfoInTable(rpsInfo, row);
        }
        rpsInfo.closeContentstram();
      }
      doc.save(byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    }
  }

  private void addLogo(PDDocument doc, PDPageContentStream cs, float pageHeight)
      throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    if (classLoader == null) {
      throw new BaseException(
          "Unable to load logo: ClassLoader is null", null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    try (InputStream is = classLoader.getResourceAsStream(Paths.LOGO_PATH)) {
      if (is == null) {
        throw new BaseException(
            "Logo image not found at path: " + Paths.LOGO_PATH, null, HttpStatus.NOT_FOUND);
      }

      PDImageXObject logoImage = PDImageXObject.createFromByteArray(doc, is.readAllBytes(), "logo");
      cs.drawImage(logoImage, 12, pageHeight - 150, 370, 100);
    } catch (IOException e) {
      throw new BaseException(
          "Failed to add logo to PDF: " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private void addHeading(PDPageContentStream cs, float pageWidth, float pageHeight)
      throws IOException {
    float fontSize = 25;
    float titleWidth = Fonts.HEADING_FONT.getStringWidth(Labels.HEADING) / 1000 * fontSize;

    cs.setNonStrokingColor(new Color(0, 150, 255));
    cs.addRect(12, pageHeight - 200, pageWidth - 20, 35);
    cs.fill();

    cs.beginText();
    cs.setNonStrokingColor(Color.WHITE);
    cs.setFont(Fonts.HEADING_FONT, fontSize);
    cs.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - 190);
    cs.showText(Labels.HEADING);
    cs.endText();
  }

  private String getTenure(String loanAccountNumber, ClientDetailsCpResponseDto clientDetails) {
    if (StringUtils.isBlank(loanAccountNumber)
        || clientDetails == null
        || clientDetails.getLoanAccountNumber() == null
        || clientDetails.getTenure() == null
        || clientDetails.getRepaymentPeriodFrequencyEnum() == null) {
      return StringUtils.EMPTY;
    }

    String inputAcc = loanAccountNumber.replaceFirst("^0+(?!$)", "");
    String clientAcc = clientDetails.getLoanAccountNumber().replaceFirst("^0+(?!$)", "");

    if (!inputAcc.equalsIgnoreCase(clientAcc)) {
      return StringUtils.EMPTY;
    }

    try {
      int frequencyValue = Integer.parseInt(clientDetails.getRepaymentPeriodFrequencyEnum());
      return LoanDetailsUtil.getRepaymentPeriodString(
          BigInteger.valueOf(clientDetails.getTenure()), frequencyValue);
    } catch (NumberFormatException e) {
      return StringUtils.EMPTY;
    }
  }

  private List<String> getCustomerDetail(
      String tenure, ResponseRpsDTO dto, ClientDetailsCpResponseDto loanDetails) {
    List<String> list = new ArrayList<>();
    list.add(Labels.CUSTOMER_NAME);
    list.add(loanDetails.getName());
    list.add(Labels.LOAN_ACCOUNT_ID);
    list.add(String.valueOf(loanDetails.getLoanAccountNumber()));
    list.add(Labels.TENURE);
    list.add(tenure);
    list.add(Labels.LOAN_AMOUNT);
    list.add(String.valueOf(dto.getProposedPrincipal()));
    list.add(Labels.REPAYMENT_FREQUENCY);
    list.add(String.valueOf(loanDetails.getRepaymentPeriodFrequency()));
    list.add(Labels.INTEREST_RATE);
    list.add(dto.getCurrentInterestRate() + " %");
    list.add(Labels.PARTNER_NAME);
    list.add(
        String.valueOf(
            ProductOfficeMapping.getOfficeNameByProductId(loanDetails.getProductId().intValue())));
    list.add(Labels.NUM_INSTALLMENTS);
    list.add(String.valueOf(dto.getNumberOfRepayments().intValue()));

    return list;
  }

  private List<List<Integer>> getRpsTotalColumnNum(Double waived) {
    return List.of(
        waived == 0.0
            ? Arrays.asList(150, 150, 150, 120, 160, 120, 120, 110, 180, 150, 120, 130)
            : Arrays.asList(150, 140, 140, 110, 150, 90, 100, 110, 150, 160, 100, 130, 130));
  }

  private List<String> getRpsHeaderData(Double waived) {
    List<String> headers = new ArrayList<>(List.of(Labels.RPS_HEADER_BASE));
    if (waived > 0.0) headers.add(8, Labels.WAIVED);
    return headers;
  }

  private List<String> getRpsTableData(
      String status,
      ResponseRpsDTO t1,
      Double waived,
      PeriodsItem p,
      AtomicReference<Integer> n,
      Double totalWaived) {

    List<String> rpsData = new ArrayList<>();

    rpsData.add(String.valueOf(p.getPeriod()));
    rpsData.add(DateTimeUtil.getDateAsMonthInString(p.getDueDate()));
    rpsData.add(
        String.valueOf(
            (p.getPrincipalDue() != null ? p.getPrincipalDue() : 0.0)
                + p.getPrincipalLoanBalanceOutstanding()));
    rpsData.add(
        String.valueOf(
            (p.getPrincipalDue() != null ? p.getPrincipalDue() : 0.0)
                + (p.getInterestOriginalDue() != null ? p.getInterestOriginalDue() : 0.0)));
    rpsData.add(String.valueOf(p.getPrincipalDue()));
    rpsData.add(String.valueOf(p.getInterestOriginalDue()));
    rpsData.add(String.valueOf(p.getFeeChargesDue()));
    rpsData.add(String.valueOf(p.getPenaltyChargesDue()));
    rpsData.add(String.valueOf(p.getPrincipalLoanBalanceOutstanding()));
    rpsData.add(
        t1.getTimeline().getExpectedDisbursementDate() != null
                && t1.getTimeline().getExpectedDisbursementDate().equals(p.getDueDate())
            ? "-"
            : p.getEmiClearedDate() != null
                ? "Paid"
                : ((p.getPrincipalPaid() != null && p.getPrincipalPaid() > 0.0)
                        || (p.getInterestPaid() != null && p.getInterestPaid() > 0.0))
                    ? "Partially Paid"
                    : "Due");
    rpsData.add(String.valueOf(p.getTotalOutstandingForPeriod()));
    rpsData.add(DateTimeUtil.getDateAsMonthInString(p.getObligationsMetOnDate()));
    if (waived > 0.0) rpsData.add(8, String.valueOf(p.getTotalWaivedForPeriod()));
    return rpsData;
  }
}
