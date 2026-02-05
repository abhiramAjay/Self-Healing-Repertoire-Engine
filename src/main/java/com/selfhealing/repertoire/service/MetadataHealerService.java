package com.selfhealing.repertoire.service;

import com.selfhealing.repertoire.client.MusicBrainzClient;
import com.selfhealing.repertoire.model.Recording;
import com.selfhealing.repertoire.model.Work;
import com.selfhealing.repertoire.repository.RecordingRepository;
import com.selfhealing.repertoire.repository.WorkRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MetadataHealerService {

    @Autowired
    private RecordingRepository recordingRepository;

    @Autowired
    private WorkRepository workRepository;

    @Autowired
    private MusicBrainzClient musicBrainzClient;

    @Autowired
    private com.selfhealing.repertoire.client.SpotifyClient spotifyClient;

    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    // Threshold for fuzzy matching (0.0 to 1.0)
    private static final double MATCH_THRESHOLD = 0.9;

    /**
     * Run this process based on configuration, defaults to 1 hour.
     */
    @Scheduled(fixedRateString = "${healer.rate:3600000}")
    public HealingReport performHealing() {
        return performHealing(null);
    }

    public HealingReport performHealing(List<java.util.UUID> selectedIds) {
        log.info("Starting metadata healing process...");
        List<Recording> recordingsToHeal;

        if (selectedIds != null && !selectedIds.isEmpty()) {
            recordingsToHeal = recordingRepository.findAllById(selectedIds);
            log.info("Healing {} selected recordings.", recordingsToHeal.size());
        } else {
            recordingsToHeal = recordingRepository.findByWorkIsNull();
            log.info("Healing {} orphaned recordings.", recordingsToHeal.size());
        }

        log.info("Found {} recordings to heal.", recordingsToHeal.size());

        HealingReport report = new HealingReport();
        report.setTotalOrphans(recordingsToHeal.size());

        for (Recording recording : recordingsToHeal) {
            String result = healRecording(recording, report);
            report.addDetail(recording.getRecordingTitle(), result);
        }

        report.setRemainingOrphans(
                report.getTotalOrphans() - (report.getHealedByDirectMatch() + report.getHealedByFuzzyMatch()));
        log.info("Metadata healing process completed. Stats: {}", report);
        return report;
    }

    /**
     * Heal a specific recording immediately (for single repair requests)
     */
    @Transactional
    public String healSpecificRecording(Recording recording) {
        HealingReport report = new HealingReport();
        return healRecording(recording, report);
    }

    @Transactional
    protected String healRecording(Recording recording, HealingReport report) {
        try {
            // Update status to indicate processing
            recording.setStatus("HEALING");
            recordingRepository.saveAndFlush(recording); // Commit immediately so UI sees it

            // Step 1: MusicBrainz (Primary - High Confidence ISRC to ISWC)
            String iswc = musicBrainzClient.findIswcByIsrc(recording.getIsrc());

            if (iswc != null) {
                return linkOrCreateWork(recording, iswc, "MusicBrainz ID Match", report);
            }

            // Step 2: Fallback to Spotify to enrich metadata for a better fuzzy match
            /*
             * USER REQUEST: Hold on Spotify fetching for now.
             * log.
             * info("MusicBrainz ISRC lookup failed for {}. Falling back to Spotify enrichment."
             * , recording.getIsrc());
             * com.selfhealing.repertoire.client.SpotifyClient.SpotifyMetadata enrichedData
             * = spotifyClient
             * .getMetadataByIsrc(recording.getIsrc());
             * 
             * if (enrichedData != null) {
             * updateRecordingMetadata(recording, enrichedData);
             * 
             * // Step 3: Use enriched metadata for a high-accuracy Title Search on
             * MusicBrainz
             * // (Deep Search)
             * log.
             * info("Performing Deep Search on MusicBrainz using enriched metadata: {} by {}"
             * ,
             * enrichedData.getTitle(), enrichedData.getArtist());
             * iswc = musicBrainzClient.findIswcByTitleAndArtist(enrichedData.getTitle(),
             * enrichedData.getArtist());
             * 
             * if (iswc != null) {
             * return linkOrCreateWork(recording, iswc,
             * "MusicBrainz Deep Search (via Spotify Enrichment)", report);
             * }
             * }
             */

            // Step 3 (Modified): Deep Search on MusicBrainz using ORIGINAL metadata (since
            // Spotify is skipped)
            if (recording.getRecordingTitle() != null && !recording.getRecordingTitle().isEmpty()) {
                log.info("Performing Deep Search on MusicBrainz using ORIGINAL metadata: {} by {}",
                        recording.getRecordingTitle(), recording.getArtistName());
                iswc = musicBrainzClient.findIswcByTitleAndArtist(recording.getRecordingTitle(),
                        recording.getArtistName());

                if (iswc != null) {
                    return linkOrCreateWork(recording, iswc, "MusicBrainz Deep Search (Original Metadata)", report);
                }
            }

            // Tier 4: Local Fuzzy Metadata Match (Last Resort)
            List<Work> allWorks = workRepository.findAll();
            Work bestMatch = null;
            double highestScore = 0.0;

            for (Work work : allWorks) {
                double score = calculateSimilarity(recording.getRecordingTitle(), work.getTitle());
                if (score > highestScore) {
                    highestScore = score;
                    bestMatch = work;
                }
            }

            if (bestMatch != null && highestScore >= MATCH_THRESHOLD) {
                linkToWork(recording, bestMatch, "Local Fuzzy Match (Score: " + highestScore + ")");
                report.setHealedByFuzzyMatch(report.getHealedByFuzzyMatch() + 1);
                recording.setStatus("HEALED");
                recordingRepository.save(recording);
                return "Healed via Local Fuzzy Match (" + String.format("%.2f", highestScore) + ")";
            } else {
                log.warn("Deep search failed for ISRC: {}", recording.getIsrc());
                recording.setStatus("ORPHANED");
                recordingRepository.save(recording);
                return "No match found";
            }

        } catch (Exception e) {
            log.error("Error healing recording {}: {}", recording.getIsrc(), e.getMessage());
            recording.setStatus("ERROR");
            recording.setDiscoverySource("Connection Interrupted");
            recordingRepository.save(recording);
            return "Connection Interrupted";
        }
    }

    private String linkOrCreateWork(Recording recording, String iswc, String method, HealingReport report) {
        Optional<Work> workOpt = workRepository.findByIswc(iswc);

        if (workOpt.isPresent()) {
            linkToWork(recording, workOpt.get(), method);
            report.setHealedByDirectMatch(report.getHealedByDirectMatch() + 1);
            recording.setStatus("HEALED");
            recordingRepository.save(recording);
            return "Healed via " + method;
        } else {
            // DISCOVERY: ISWC found but Work doesn't exist locally - create it!
            Work newWork = new Work();
            newWork.setIswc(iswc);
            newWork.setTitle(recording.getRecordingTitle());
            newWork.setWorkType("Original");
            newWork = workRepository.save(newWork);

            linkToWork(recording, newWork, "Discovered via " + method);
            report.setHealedByDirectMatch(report.getHealedByDirectMatch() + 1);
            log.info("✨ DISCOVERY: Created new Work '{}' with ISWC {} from {}",
                    newWork.getTitle(), iswc, method);

            recording.setStatus("HEALED");
            recordingRepository.save(recording);

            return "Discovered & Created via " + method;
        }
    }

    private void updateRecordingMetadata(Recording recording,
            com.selfhealing.repertoire.client.SpotifyClient.SpotifyMetadata enrichedData) {
        log.info("Updating local metadata for {} using Spotify enrichment", recording.getIsrc());
        boolean changed = false;
        if (recording.getRecordingTitle() == null || recording.getRecordingTitle().isEmpty()) {
            recording.setRecordingTitle(enrichedData.getTitle());
            changed = true;
        }
        if (recording.getArtistName() == null || recording.getArtistName().isEmpty()) {
            recording.setArtistName(enrichedData.getArtist());
            changed = true;
        }
        if (recording.getDurationMs() == null || recording.getDurationMs() == 0) {
            recording.setDurationMs(enrichedData.getDurationMs());
            changed = true;
        }
        if (changed) {
            recording.setDiscoverySource("Spotify Metadata Enrichment");
            recordingRepository.save(recording);
        }
    }

    private void linkToWork(Recording recording, Work work, String method) {
        recording.setWork(work);
        recording.setDiscoverySource(method);
        recordingRepository.save(recording);
        log.info("Healed: Linked Recording '{}' to Work '{}' via {}",
                recording.getRecordingTitle(), work.getTitle(), method);
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null)
            return 0.0;

        // Jaro-Winkler is often good for short strings like titles/names
        return jaroWinkler.apply(s1, s2);

        // Alternatively, use Levenshtein normalized to 0-1 range if preferred
        // int distance = levenshtein.apply(s1, s2);
        // return 1.0 - ((double) distance / Math.max(s1.length(), s2.length()));
    }
}
