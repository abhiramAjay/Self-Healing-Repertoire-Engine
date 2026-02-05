package com.selfhealing.repertoire.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Entity
@Table(name = "recordings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recording {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(unique = true, length = 12)
    private String isrc;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "work_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Work work;

    @Column(name = "recording_title", nullable = false)
    private String recordingTitle;

    @Column(name = "artist_name")
    private String artistName;

    @Column(name = "discovery_source")
    private String discoverySource;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "updated_at")
    @org.hibernate.annotations.UpdateTimestamp
    private java.time.LocalDateTime updatedAt;
}
