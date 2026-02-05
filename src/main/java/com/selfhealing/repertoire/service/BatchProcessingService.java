package com.selfhealing.repertoire.service;

import com.opencsv.CSVReader;
import com.selfhealing.repertoire.model.Recording;
import com.selfhealing.repertoire.repository.RecordingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Service
public class BatchProcessingService {

    @Autowired
    private RecordingRepository recordingRepository;

    @Autowired
    private MetadataHealerService healerService;

    @Async
    @org.springframework.transaction.annotation.Transactional
    public void processRecords(java.util.List<String[]> records, boolean heal) {
        try {
            int count = 0;
            System.out.println("🚀 Background Task: Processing " + records.size() + " records...");
            for (String[] line : records) {
                if (line.length >= 1 && line[0] != null && !line[0].trim().isEmpty()) {
                    String isrc = (line.length >= 3 && line[2] != null) ? line[2].trim() : null;

                    Recording rec = null;
                    if (isrc != null && !isrc.isEmpty()) {
                        java.util.List<Recording> existing = recordingRepository.findByIsrc(isrc);
                        if (!existing.isEmpty()) {
                            rec = existing.get(0);
                        }
                    }

                    if (rec == null) {
                        rec = new Recording();
                    }

                    rec.setRecordingTitle(line[0].trim()); // Column 1: Title

                    // Column 2: Artist (Populating the new field)
                    if (line.length >= 2 && line[1] != null && !line[1].trim().isEmpty()) {
                        rec.setArtistName(line[1].trim());
                    }

                    // Column 3: ISRC (optional)
                    if (line.length >= 3 && line[2] != null && !line[2].trim().isEmpty()) {
                        rec.setIsrc(line[2].trim());
                    }

                    recordingRepository.save(rec);
                    System.out.println(" ✅ Saved record: " + rec.getRecordingTitle());
                    count++;
                } else {
                    System.out.println(" ⚠️ Skipping invalid or empty row in CSV: " + java.util.Arrays.toString(line));
                }
            }

            System.out.println("Batch processing completed. " + count + " records added.");

            // Trigger healing for all new orphan records (if enabled)
            if (heal) {
                System.out.println("Starting healing process...");
                healerService.performHealing();
            } else {
                System.out.println("Healing skipped - import only mode");
            }

        } catch (Exception e) {
            System.err.println("Error processing batch records: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
