package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ExportService {
    
    /**
     * Export all events to CSV format
     */
    byte[] exportEventsToCSV() throws IOException;
    
    /**
     * Export all events to JSON format
     */
    byte[] exportEventsToJSON() throws IOException;
    
    /**
     * Export all users/volunteers to CSV format
     */
    byte[] exportUsersToCSV() throws IOException;
    
    /**
     * Export all users/volunteers to JSON format
     */
    byte[] exportUsersToJSON() throws IOException;
    
    /**
     * Export events with filters to CSV
     */
    byte[] exportEventsToCSV(String status, String startDate, String endDate) throws IOException;
    
    /**
     * Export users with filters to CSV
     */
    byte[] exportUsersToCSV(String role, Boolean enabled) throws IOException;
}

