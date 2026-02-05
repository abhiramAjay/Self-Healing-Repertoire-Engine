package com.selfhealing.repertoire.bootstrap;

import com.selfhealing.repertoire.model.Recording;
import com.selfhealing.repertoire.model.Work;
import com.selfhealing.repertoire.repository.RecordingRepository;
import com.selfhealing.repertoire.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final WorkRepository workRepository;
    private final RecordingRepository recordingRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (workRepository.count() == 0 && recordingRepository.count() == 0) {
            log.info("Bootstrapping data for Self-Healing Demo...");
            seedData();
        }
    }

    private void seedData() {
        // 1. Seed a Work for Fuzzy Matching comparison
        Work work = new Work();
        work.setTitle("Bohemian Rhapsody");
        work.setIswc("T-010.154.444-5"); // Example ISWC
        work.setWorkType("Original");
        workRepository.save(work);

        // 2. Seed an ORPHAN Recording (No Work) that needs Tier 1 Healing (ISRC ->
        // ISWC)
        // Using a real ISRC for testing (Ed Sheeran - Shape of You)
        Recording orphanViaIsrc = new Recording();
        orphanViaIsrc.setRecordingTitle("Shape of You - Demo");
        orphanViaIsrc.setIsrc("GBAHS1600463");
        orphanViaIsrc.setDurationMs(233760);
        recordingRepository.save(orphanViaIsrc);

        // 3. Seed an ORPHAN Recording (No Work, No ISRC) that needs Tier 2 Healing
        // (Fuzzy Match)
        Recording orphanViaFuzzy = new Recording();
        orphanViaFuzzy.setRecordingTitle("Bohemian Rapsody"); // Intentional Typo for fuzzy match
        orphanViaFuzzy.setDurationMs(354000);
        // No ISRC, No Work
        recordingRepository.save(orphanViaFuzzy);

        log.info("Data Seeded: 1 Work, 2 Orphan Recordings ready for healing.");
    }
}
