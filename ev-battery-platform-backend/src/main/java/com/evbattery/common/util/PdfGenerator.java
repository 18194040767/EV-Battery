package com.evbattery.common.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfGenerator {
    private PdfGenerator() {
    }

    public static byte[] generateSimpleDocument(String title, List<String> lines) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;
            writeLine(contentStream, title, 16, margin, y, true);
            y -= 28;
            for (String line : lines) {
                if (y < 60) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - margin;
                }
                writeLine(contentStream, line, 10, margin, y, false);
                y -= 18;
            }
            contentStream.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate PDF", ex);
        }
    }

    public static void writeToFile(File file, byte[] data) {
        if (file == null) {
            throw new IllegalArgumentException("Target file must not be null");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create directory: " + parent.getAbsolutePath());
        }
        try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file)) {
            outputStream.write(data);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write PDF file", ex);
        }
    }

    private static void writeLine(PDPageContentStream contentStream, String text, int fontSize, float x, float y, boolean bold) throws IOException {
        contentStream.beginText();
        contentStream.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text == null ? "" : text);
        contentStream.endText();
    }
}
