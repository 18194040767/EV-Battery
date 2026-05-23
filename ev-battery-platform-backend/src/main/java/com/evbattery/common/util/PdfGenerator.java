package com.evbattery.common.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PdfGenerator {
    private PdfGenerator() {
    }

    public static byte[] generateSimpleDocument(String title, List<String> lines) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDFont font = loadReadableFont(document, false);
            PDFont boldFont = loadReadableFont(document, true);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;
            writeLine(contentStream, title, 16, margin, y, boldFont);
            y -= 32;
            for (String line : expandLines(lines, font, 10, page.getMediaBox().getWidth() - margin * 2)) {
                if (y < 60) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - margin;
                }
                writeLine(contentStream, line, 10, margin, y, font);
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

    private static PDFont loadReadableFont(PDDocument document, boolean bold) throws IOException {
        List<String> candidates = bold
                ? Arrays.asList(
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyhbd.ttc",
                "C:/Windows/Fonts/simsunb.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Bold.ttc"
        )
                : Arrays.asList(
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simsun.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"
        );
        for (String path : candidates) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    return PDType0Font.load(document, file);
                } catch (IOException ignored) {
                    // Try the next candidate font.
                }
            }
        }
        return bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
    }

    private static List<String> expandLines(List<String> lines, PDFont font, int fontSize, float maxWidth) throws IOException {
        List<String> result = new ArrayList<String>();
        if (lines == null) {
            return result;
        }
        for (String line : lines) {
            result.addAll(wrapLine(line == null ? "" : line, font, fontSize, maxWidth));
        }
        return result;
    }

    private static List<String> wrapLine(String line, PDFont font, int fontSize, float maxWidth) throws IOException {
        List<String> wrapped = new ArrayList<String>();
        String normalized = line.replace("\r", "").replace("\t", "    ");
        if (normalized.isEmpty()) {
            wrapped.add("");
            return wrapped;
        }
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            String candidate = current.toString() + ch;
            if (stringWidth(candidate, font, fontSize) > maxWidth && current.length() > 0) {
                wrapped.add(current.toString());
                current.setLength(0);
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            wrapped.add(current.toString());
        }
        return wrapped;
    }

    private static float stringWidth(String text, PDFont font, int fontSize) throws IOException {
        return font.getStringWidth(text == null ? "" : text) / 1000F * fontSize;
    }

    private static void writeLine(PDPageContentStream contentStream, String text, int fontSize, float x, float y, PDFont font) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text == null ? "" : text);
        contentStream.endText();
    }
}
