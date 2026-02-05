package com.selfhealing.repertoire.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordingRequest {
    private String title;
    private String artist;
    private String isrc;
}
