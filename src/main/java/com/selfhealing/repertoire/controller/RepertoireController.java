package com.selfhealing.repertoire.controller;

import com.selfhealing.repertoire.service.HealingReport;
import com.selfhealing.repertoire.service.MetadataHealerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
@RestController
@RequestMapping("/api/repertoire")
public class RepertoireController {

    private final MetadataHealerService healerService;
    private final com.selfhealing.repertoire.repository.RecordingRepository recordingRepository;
    private final com.selfhealing.repertoire.repository.WorkRepository workRepository;
    private final com.selfhealing.repertoire.service.BatchProcessingService batchProcessingService;

    @Autowired
    public RepertoireController(MetadataHealerService healerService,
            com.selfhealing.repertoire.repository.RecordingRepository recordingRepository,
            com.selfhealing.repertoire.repository.WorkRepository workRepository,
            com.selfhealing.repertoire.service.BatchProcessingService batchProcessingService) {
        this.healerService = healerService;
        this.recordingRepository = recordingRepository;
        this.workRepository = workRepository;
        this.batchProcessingService = batchProcessingService;
    }

    @PostMapping("/heal-now")
    public ResponseEntity<String> manualHeal(
            @org.springframework.web.bind.annotation.RequestBody(required = false) java.util.List<java.util.UUID> selectedIds) {
        // Trigger healing in a separate thread to avoid blocking the UI
        new Thread(() -> healerService.performHealing(selectedIds)).start();
        return ResponseEntity.accepted().body("Healing job started in background.");
    }

    @DeleteMapping("/recordings")
    public ResponseEntity<Void> deleteRecordings(
            @org.springframework.web.bind.annotation.RequestBody java.util.List<java.util.UUID> ids) {
        recordingRepository.deleteAllById(ids);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<com.selfhealing.repertoire.dto.DashboardStats> getStats() {
        long total = recordingRepository.count();
        long orphaned = recordingRepository.countByWorkIsNull();
        long healed = total - orphaned;
        double revenue = healed * 0.05;

        com.selfhealing.repertoire.dto.DashboardStats stats = new com.selfhealing.repertoire.dto.DashboardStats(
                total, orphaned, healed, revenue);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recordings")
    public ResponseEntity<java.util.List<com.selfhealing.repertoire.dto.RecordingDTO>> getAllRecordings() {
        java.util.List<com.selfhealing.repertoire.model.Recording> recordings = recordingRepository.findAll();
        java.util.List<com.selfhealing.repertoire.dto.RecordingDTO> dtos = recordings.stream()
                .map(rec -> {
                    com.selfhealing.repertoire.dto.RecordingDTO dto = new com.selfhealing.repertoire.dto.RecordingDTO();
                    dto.setId(rec.getId());
                    dto.setIsrc(rec.getIsrc());
                    dto.setRecordingTitle(rec.getRecordingTitle());
                    dto.setArtistName(rec.getArtistName());
                    dto.setDurationMs(rec.getDurationMs());
                    dto.setStatus(rec.getStatus());
                    dto.setUpdatedAt(rec.getUpdatedAt());
                    if (rec.getWork() != null) {
                        com.selfhealing.repertoire.dto.RecordingDTO.WorkDTO workDTO = new com.selfhealing.repertoire.dto.RecordingDTO.WorkDTO();
                        workDTO.setId(rec.getWork().getId());
                        workDTO.setIswc(rec.getWork().getIswc());
                        workDTO.setTitle(rec.getWork().getTitle());
                        workDTO.setWorkType(rec.getWork().getWorkType());
                        dto.setWork(workDTO);
                    }
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/works/search")
    public ResponseEntity<java.util.List<com.selfhealing.repertoire.model.Work>> searchWorks(
            @org.springframework.web.bind.annotation.RequestParam String query) {
        return ResponseEntity
                .ok(workRepository.findByTitleContainingIgnoreCaseOrIswcContainingIgnoreCase(query, query));
    }

    @org.springframework.web.bind.annotation.PutMapping("/recordings/{recId}/link/{workId}")
    public ResponseEntity<Void> manualLink(
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID recId,
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID workId) {
        com.selfhealing.repertoire.model.Recording rec = recordingRepository.findById(recId)
                .orElseThrow(() -> new RuntimeException("Recording not found"));
        com.selfhealing.repertoire.model.Work work = workRepository.findById(workId)
                .orElseThrow(() -> new RuntimeException("Work not found"));
        rec.setWork(work);
        recordingRepository.save(rec);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/repair/single")
    public ResponseEntity<com.selfhealing.repertoire.dto.RecordingDTO> repairSingle(
            @org.springframework.web.bind.annotation.RequestBody com.selfhealing.repertoire.dto.RecordingRequest request,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "true") boolean heal) {
        try {
            // Validate input
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Create or update orphan recording
            com.selfhealing.repertoire.model.Recording rec = null;
            if (request.getIsrc() != null && !request.getIsrc().trim().isEmpty()) {
                java.util.List<com.selfhealing.repertoire.model.Recording> existing = recordingRepository
                        .findByIsrc(request.getIsrc().trim());
                if (!existing.isEmpty()) {
                    rec = existing.get(0);
                    System.out.println("Updating existing recording: " + rec.getIsrc());
                }
            }

            if (rec == null) {
                rec = new com.selfhealing.repertoire.model.Recording();
            }

            rec.setRecordingTitle(request.getTitle().trim());
            if (request.getArtist() != null && !request.getArtist().trim().isEmpty()) {
                rec.setArtistName(request.getArtist().trim());
            }
            if (request.getIsrc() != null && !request.getIsrc().trim().isEmpty()) {
                rec.setIsrc(request.getIsrc().trim());
            }
            rec = recordingRepository.save(rec);

            // Trigger IMMEDIATE healing with discovery for this specific record (if
            // enabled)
            if (heal) {
                String healResult = healerService.healSpecificRecording(rec);
                System.out.println("Heal result: " + healResult);
            } else {
                System.out.println("Healing skipped - import only mode");
            }

            // Fetch the updated recording with eager loading
            rec = recordingRepository.findById(rec.getId()).orElse(rec);

            // Convert to DTO
            com.selfhealing.repertoire.dto.RecordingDTO dto = new com.selfhealing.repertoire.dto.RecordingDTO();
            dto.setId(rec.getId());
            dto.setIsrc(rec.getIsrc());
            dto.setRecordingTitle(rec.getRecordingTitle());
            dto.setArtistName(rec.getArtistName());
            dto.setDurationMs(rec.getDurationMs());

            if (rec.getWork() != null) {
                com.selfhealing.repertoire.dto.RecordingDTO.WorkDTO workDTO = new com.selfhealing.repertoire.dto.RecordingDTO.WorkDTO();
                workDTO.setId(rec.getWork().getId());
                workDTO.setIswc(rec.getWork().getIswc());
                workDTO.setTitle(rec.getWork().getTitle());
                workDTO.setWorkType(rec.getWork().getWorkType());
                dto.setWork(workDTO);
            }

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            System.err.println("Error in repair/single: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/repair/batch")
    public ResponseEntity<String> repairBatch(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "true") boolean heal) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a valid CSV file.");
        }

        try {
            java.util.List<String[]> records = new java.util.ArrayList<>();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(file.getInputStream()));
                    com.opencsv.CSVReader csvReader = new com.opencsv.CSVReader(reader)) {

                csvReader.readNext(); // Skip header
                String[] nextLine;
                while ((nextLine = csvReader.readNext()) != null) {
                    records.add(nextLine);
                }
            }

            if (records.isEmpty()) {
                return ResponseEntity.status(400).body("The CSV file is empty or only contains the header row.");
            }

            System.out.println("Parsed " + records.size() + " records from " + file.getOriginalFilename());
            batchProcessingService.processRecords(records, heal);

            String message = heal
                    ? "Batch repair started for: " + file.getOriginalFilename()
                            + ". Processing and healing in background."
                    : "Batch import started for: " + file.getOriginalFilename() + ". Importing without healing.";
            return ResponseEntity.ok(message + " Refresh the dashboard to see progress.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to process file: " + e.getMessage());
        }
    }
}
