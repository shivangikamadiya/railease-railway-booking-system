package com.railease.service;

import com.railease.entity.Ticket;

import java.io.ByteArrayOutputStream;

/**
 * Service interface for generating PDF tickets
 */
public interface PdfService {
    
    /**
     * Generate a PDF ticket for the given ticket
     * @param ticket The ticket to generate PDF for
     * @return ByteArrayOutputStream containing the PDF content
     */
    ByteArrayOutputStream generateTicketPdf(Ticket ticket);
    
    /**
     * Generate a PDF ticket and return as byte array
     * @param ticket The ticket to generate PDF for
     * @return byte array containing the PDF content
     */
    byte[] generateTicketPdfAsBytes(Ticket ticket);
}