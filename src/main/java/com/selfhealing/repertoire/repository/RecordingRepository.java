package com.selfhealing.repertoire.repository;

import com.selfhealing.repertoire.model.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecordingRepository extends JpaRepository<Recording, UUID> {
    List<Recording> findByWorkIsNull();

    List<Recording> findByIsrc(String isrc);

    long countByWorkIsNull();
}
