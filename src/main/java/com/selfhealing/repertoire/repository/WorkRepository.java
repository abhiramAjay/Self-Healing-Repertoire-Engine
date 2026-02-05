package com.selfhealing.repertoire.repository;

import com.selfhealing.repertoire.model.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkRepository extends JpaRepository<Work, UUID> {
    Optional<Work> findByIswc(String iswc);

    // Needed for fuzzy matching later
    Optional<Work> findByTitleAndWorkType(String title, String workType);

    // Search for works by title or ISWC
    java.util.List<Work> findByTitleContainingIgnoreCaseOrIswcContainingIgnoreCase(String title, String iswc);
}
