package com.trillionloans.customer_portal.util.RpsDocumentUtil;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

public class RpsTable {

  private final PDDocument pdDocument;
  private PDPageContentStream contentStream;
  private List<Integer> colWidth;
  private int cellHeight;
  private int xPosition;
  private int yPosition;
  private int colPosition = 0;
  private int xInitialPosition;
  private float fontSize;
  private PDFont font;
  private Color fontColor;
  private int pageHeight;

  public RpsTable(PDDocument pdDocument, PDPageContentStream pdPageContentStream) {
    this.pdDocument = pdDocument;
    this.contentStream = pdPageContentStream;
  }

  public void setTable(
      List<Integer> colWidth,
      int cellHeight,
      int xPosition,
      int yPosition,
      int pageHeight,
      int pageWidth) {

    this.colWidth = colWidth;
    this.cellHeight = cellHeight;
    this.xInitialPosition = xPosition;
    this.yPosition = yPosition;
    this.xPosition = xPosition;
    this.pageHeight = pageHeight;
  }

  public void setTableFont(PDFont font, float fontSize, Color fontColor) {

    this.font = font;
    this.fontSize = fontSize;
    this.fontColor = fontColor;
  }

  public void addCell(String text, Color fillColor) throws IOException {

    contentStream.setStrokingColor(0.0f);

    if (fillColor != null) {
      contentStream.setNonStrokingColor(fillColor);
    }
    contentStream.addRect(xPosition, yPosition, colWidth.get(colPosition), cellHeight);
    if (fillColor == null) {
      contentStream.stroke();
    } else {
      contentStream.fillAndStroke();
    }
    contentStream.beginText();
    contentStream.setNonStrokingColor(fontColor);

    contentStream.setFont(font, fontSize);

    float textWidth = font.getStringWidth(text) / 1000 * fontSize;
    float cellWidth = colWidth.get(colPosition);

    float textX = xPosition + (cellWidth - textWidth) / 2;

    float textY = yPosition + (cellHeight - fontSize) / 2;

    contentStream.newLineAtOffset(textX, textY);
    contentStream.showText(text);
    contentStream.endText();

    xPosition = xPosition + colWidth.get(colPosition);
    colPosition++;
    if (colPosition == colWidth.size()) {
      colPosition = 0;
      xPosition = xInitialPosition;
      if (yPosition - cellHeight < 50) {
        contentStream.close();
        PDPage page = new PDPage(PDRectangle.A1);
        pdDocument.addPage(page);
        contentStream = new PDPageContentStream(pdDocument, page);
        yPosition = pageHeight - 50;
      } else {
        yPosition -= cellHeight;
      }
    }
  }

  public void closeContentstram() throws IOException {
    contentStream.close();
  }
}
