package com.tendanz.pricing.service;

import com.tendanz.pricing.dto.QuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to generate quote PDF exports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {

    private final PricingService pricingService;

    public byte[] generateQuotePdf(Long quoteId) {
        QuoteResponse quote = pricingService.getQuote(quoteId);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = 780f;

                y = writeLine(contentStream, "Insurance Quote", 18, y, true);
                y -= 10;
                y = writeLine(contentStream, "Quote ID: " + quote.getQuoteId(), 12, y, false);
                y = writeLine(contentStream, "Created At: " + quote.getCreatedAt(), 12, y, false);

                y -= 10;
                y = writeLine(contentStream, "Client Information", 14, y, true);
                y = writeLine(contentStream, "Name: " + quote.getClientName(), 12, y, false);
                y = writeLine(contentStream, "Age: " + quote.getClientAge(), 12, y, false);

                y -= 10;
                y = writeLine(contentStream, "Insurance Details", 14, y, true);
                y = writeLine(contentStream, "Product: " + quote.getProductName(), 12, y, false);
                y = writeLine(contentStream, "Zone: " + quote.getZoneName(), 12, y, false);

                y -= 10;
                y = writeLine(contentStream, "Pricing", 14, y, true);
                y = writeLine(contentStream, "Base Price: " + quote.getBasePrice() + " TND", 12, y, false);
                y = writeLine(contentStream, "Final Price: " + quote.getFinalPrice() + " TND", 12, y, false);

                y -= 10;
                y = writeLine(contentStream, "Applied Rules", 14, y, true);
                for (String rule : quote.getAppliedRules()) {
                    List<String> wrappedLines = wrapText("- " + rule, 95);
                    for (String line : wrappedLines) {
                        y = writeLine(contentStream, line, 11, y, false);
                    }
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for quote {}", quoteId, e);
            throw new IllegalStateException("Unable to generate PDF for quote " + quoteId);
        }
    }

    private float writeLine(PDPageContentStream contentStream, String text, int fontSize, float y, boolean bold)
            throws Exception {
        String safeText = sanitizeText(text);
        contentStream.beginText();
        contentStream.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, fontSize);
        contentStream.newLineAtOffset(50, y);
        contentStream.showText(safeText);
        contentStream.endText();
        return y - (fontSize + 6);
    }

    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text.length() <= maxLength) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() + word.length() + 1 > maxLength) {
                lines.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(word).append(' ');
        }

        if (!current.isEmpty()) {
            lines.add(current.toString().trim());
        }

        return lines;
    }

    private String sanitizeText(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        normalized = normalized
                .replace("→", "->")
                .replace("—", "-")
                .replace("–", "-");
        return normalized.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
