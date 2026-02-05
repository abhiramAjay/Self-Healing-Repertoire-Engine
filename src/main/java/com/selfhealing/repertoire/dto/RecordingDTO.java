package com.selfhealing.repertoire.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordingDTO {
    private UUID id;
    private String isrc;
    private String recordingTitle;
    private String artistName;
    private String discoverySource;
    private Integer durationMs;
    private String status;
    private java.time.LocalDateTime updatedAt;
    private WorkDTO work;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkDTO {
        private UUID id;
        private String iswc;
        private String title;
        private String workType;
    }
}
