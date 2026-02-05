package com.selfhealing.repertoire.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;

@Component
@Slf4j
public class MusicBrainzClient {
    private static final String API_URL = "https://musicbrainz.org/ws/2/recording";
    private final RestTemplate restTemplate = new RestTemplate();

    @org.springframework.beans.factory.annotation.Value("${musicbrainz.user-agent:RepertoireAI/1.0 ( contact@example.com )}")
    private String userAgent;

    @org.springframework.retry.annotation.Retryable(value = {
            org.springframework.web.client.ResourceAccessException.class,
            javax.net.ssl.SSLHandshakeException.class }, maxAttempts = 3, backoff = @org.springframework.retry.annotation.Backoff(delay = 2000, multiplier = 2))
    public String findIswcByIsrc(String isrc) {
        // PROFESSIONAL TOUCH: Enforce 1-second delay to avoid getting blocked
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted during rate limit sleep", e);
        }

        HttpHeaders headers = new HttpHeaders();
        // REQUIREMENT: Must identify your application to MusicBrainz
        headers.set("User-Agent", userAgent);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = API_URL + "?query=isrc:" + isrc + "&fmt=json";

        try {
            log.debug("Querying MusicBrainz for ISRC: {}", isrc);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return extractIswc(response.getBody());
        } catch (Exception e) {
            log.error("MusicBrainz lookup failed for ISRC {}: {}", isrc, e.getMessage());
            throw e; // Rethrow to trigger retry
        }
    }

    @org.springframework.retry.annotation.Retryable(value = {
            org.springframework.web.client.ResourceAccessException.class,
            javax.net.ssl.SSLHandshakeException.class }, maxAttempts = 3, backoff = @org.springframework.retry.annotation.Backoff(delay = 2000, multiplier = 2))
    public String findIswcByTitleAndArtist(String title, String artist) {
        // Enforce 1-second delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", userAgent);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        // RELAXED SEARCH: Removed quotes to allow fuzzy matching (e.g., "remix", "feat"
        // variations)
        String query = "recording:" + title;
        if (artist != null && !artist.trim().isEmpty()) {
            query += " AND artist:" + artist;
        }
        String url = API_URL + "?query=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                + "&fmt=json";

        try {
            log.debug("Querying MusicBrainz for Title: {} Artist: {}", title, artist);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return extractIswc(response.getBody());
        } catch (Exception e) {
            log.error("MusicBrainz lookup failed for search: {}", e.getMessage());
            throw e; // Rethrow to trigger retry
        }
    }

    @org.springframework.retry.annotation.Recover
    public String recover(Exception e, String param) {
        log.error("All retries exhausted for MusicBrainz lookup. Moving to fallback. Error: {}", e.getMessage());
        return null;
    }

    private String extractIswc(JsonNode rootNode) {
        if (rootNode == null)
            return null;

        JsonNode recordings = rootNode.path("recordings");
        if (recordings.isMissingNode() || recordings.isEmpty()) {
            return null;
        }

        for (JsonNode recording : recordings) {
            JsonNode iswcs = recording.path("iswcs");
            if (!iswcs.isMissingNode() && !iswcs.isEmpty()) {
                return iswcs.get(0).asText();
            }

            JsonNode relations = recording.path("relations");
            if (!relations.isMissingNode()) {
                for (JsonNode relation : relations) {
                    if ("work".equals(relation.path("target-type").asText())) {
                        JsonNode work = relation.path("work");
                        JsonNode workIswcs = work.path("iswcs");
                        if (!workIswcs.isMissingNode() && !workIswcs.isEmpty()) {
                            return workIswcs.get(0).asText();
                        }
                        if (work.has("iswc")) {
                            return work.get("iswc").asText();
                        }
                    }
                }
            }
        }
        return null;
    }
}
