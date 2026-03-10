package com.railease.service.impl;

import com.railease.entity.Ticket;
import com.railease.entity.Train;
import com.railease.entity.User;
import com.railease.service.PdfService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of PdfService for generating PDF tickets
 */
@Service
@Slf4j
public class PdfServiceImpl implements PdfService {

    private static final Color PRIMARY_COLOR = new Color(67, 97, 238);
    private static final Color SECONDARY_COLOR = new Color(58, 12, 163);
    private static final Color LIGHT_GRAY = new Color(248, 249, 250);

    @Override
    public ByteArrayOutputStream generateTicketPdf(Ticket ticket) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // Add content to the document
            addHeader(document, ticket);
            addTicketDetails(document, ticket);
            addPassengerDetails(document, ticket);
            addFooter(document, ticket);
            
            document.close();
            log.info("PDF generated successfully for ticket: {}", ticket.getTicketId());
            
        } catch (Exception e) {
            log.error("Error generating PDF for ticket: {}", ticket.getTicketId(), e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
        
        return baos;
    }

    @Override
    public byte[] generateTicketPdfAsBytes(Ticket ticket) {
        ByteArrayOutputStream baos = generateTicketPdf(ticket);
        return baos.toByteArray();
    }

    private void addHeader(Document document, Ticket ticket) throws DocumentException {
        // Header table
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3f, 1f});
        
        // Logo/Title
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, SECONDARY_COLOR);
        Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
        
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(10);
        
        Paragraph title = new Paragraph("RailEase", titleFont);
        title.add(new Paragraph("\nTrain Ticket", new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR)));
        title.add(new Paragraph("Indian Railways Reservation", subtitleFont));
        
        titleCell.addElement(title);
        headerTable.addCell(titleCell);
        
        // Ticket ID
        PdfPCell ticketIdCell = new PdfPCell();
        ticketIdCell.setBorder(Rectangle.NO_BORDER);
        ticketIdCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        ticketIdCell.setPadding(10);
        
        Font ticketIdFont = new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR);
        Paragraph ticketId = new Paragraph("TICKET ID", subtitleFont);
        ticketId.add(new Paragraph(ticket.getTicketId(), ticketIdFont));
        
        ticketIdCell.addElement(ticketId);
        headerTable.addCell(ticketIdCell);
        
        document.add(headerTable);
        
        // Divider line
        PdfPTable dividerTable = new PdfPTable(1);
        dividerTable.setWidthPercentage(100);
        PdfPCell dividerCell = new PdfPCell();
        dividerCell.setBackgroundColor(PRIMARY_COLOR);
        dividerCell.setFixedHeight(3);
        dividerCell.setBorder(Rectangle.NO_BORDER);
        dividerTable.addCell(dividerCell);
        document.add(dividerTable);
    }

    private void addTicketDetails(Document document, Ticket ticket) throws DocumentException {
        Font sectionTitleFont = new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.GRAY);
        Font valueFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
        
        document.add(new Paragraph("\nJourney Details", sectionTitleFont));
        document.add(new Paragraph("\n"));
        
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f, 1f, 1f});
        
        Train train = ticket.getTrain();
        
        addTableCell(table, "Train Name", labelFont);
        addTableCell(table, train.getTrainName(), valueFont);
        
        addTableCell(table, "Train Number", labelFont);
        addTableCell(table, String.valueOf(train.getTrainNo()), valueFont);
        
        addTableCell(table, "Journey Date", labelFont);
        addTableCell(table, ticket.getJourneyDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), valueFont);
        
        addTableCell(table, "Class", labelFont);
        addTableCell(table, ticket.getClassType(), valueFont);
        
        addTableCell(table, "From", labelFont);
        addTableCell(table, ticket.getSourceStation(), valueFont);
        
        addTableCell(table, "To", labelFont);
        addTableCell(table, ticket.getDestinationStation(), valueFont);
        
        addTableCell(table, "Departure Time", labelFont);
        addTableCell(table, train.getDepartureTime() != null ? train.getDepartureTime().toString() : "N/A", valueFont);
        
        addTableCell(table, "Arrival Time", labelFont);
        addTableCell(table, train.getArrivalTime() != null ? train.getArrivalTime().toString() : "N/A", valueFont);
        
        document.add(table);
    }

    private void addPassengerDetails(Document document, Ticket ticket) throws DocumentException {
        Font sectionTitleFont = new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.GRAY);
        Font valueFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
        
        document.add(new Paragraph("\n\nPassenger Details", sectionTitleFont));
        document.add(new Paragraph("\n"));
        
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f, 1f, 1f});
        
        User user = ticket.getUser();
        
        addTableCell(table, "Passenger Name", labelFont);
        addTableCell(table, ticket.getPassengerName(), valueFont);
        
        addTableCell(table, "Age", labelFont);
        addTableCell(table, String.valueOf(ticket.getPassengerAge()), valueFont);
        
        addTableCell(table, "Gender", labelFont);
        addTableCell(table, ticket.getPassengerGender(), valueFont);
        
        addTableCell(table, "No. of Seats", labelFont);
        addTableCell(table, String.valueOf(ticket.getNumberOfSeats()), valueFont);
        
        addTableCell(table, "Booking Date", labelFont);
        addTableCell(table, ticket.getBookingDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")), valueFont);
        
        addTableCell(table, "Payment Status", labelFont);
        addTableCell(table, ticket.getPaymentStatus(), valueFont);
        
        addTableCell(table, "Booking Status", labelFont);
        addTableCell(table, ticket.getBookingStatus(), valueFont);
        
        addTableCell(table, "Payment Method", labelFont);
        addTableCell(table, ticket.getPaymentMethod() != null ? ticket.getPaymentMethod() : "N/A", valueFont);
        
        document.add(table);
        
        // Fare Details
        document.add(new Paragraph("\n\nFare Details", sectionTitleFont));
        document.add(new Paragraph("\n"));
        
        PdfPTable fareTable = new PdfPTable(2);
        fareTable.setWidthPercentage(50);
        fareTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        
        Font fareLabelFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.GRAY);
        Font fareValueFont = new Font(Font.HELVETICA, 14, Font.BOLD, SECONDARY_COLOR);
        
        addTableCell(fareTable, "Total Fare (Including GST)", fareLabelFont);
        addTableCell(fareTable, "₹" + String.format("%.2f", ticket.getTotalFare()), fareValueFont);
        
        document.add(fareTable);
    }

    private void addFooter(Document document, Ticket ticket) throws DocumentException {
        document.add(new Paragraph("\n\n"));
        
        // Divider
        PdfPTable dividerTable = new PdfPTable(1);
        dividerTable.setWidthPercentage(100);
        PdfPCell dividerCell = new PdfPCell();
        dividerCell.setBackgroundColor(LIGHT_GRAY);
        dividerCell.setFixedHeight(1);
        dividerCell.setBorder(Rectangle.NO_BORDER);
        dividerTable.addCell(dividerCell);
        document.add(dividerTable);
        
        // Footer text
        Font footerFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Paragraph footer = new Paragraph("\n\nThis is a computer generated ticket and does not require signature.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
        
        Paragraph terms = new Paragraph("\nTerms and Conditions:\n" +
                "1. Please carry a valid ID proof along with this ticket.\n" +
                "2. This ticket is non-transferable.\n" +
                "3. Cancellation is subject to railway refund policy.\n" +
                "4. Please arrive at the station at least 30 minutes before departure.", footerFont);
        terms.setAlignment(Element.ALIGN_LEFT);
        document.add(terms);
        
        // QR Code placeholder note
        Paragraph qrNote = new Paragraph("\n\nFor any assistance, contact RailEase Customer Care: 1800-XXX-XXXX", 
                new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY_COLOR));
        qrNote.setAlignment(Element.ALIGN_CENTER);
        document.add(qrNote);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(10);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }
}