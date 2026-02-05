package com.selfhealing.repertoire.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class SpotifyClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private final String SEARCH_URL = "https://api.spotify.com/v1/search";

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private String accessToken = null;
    private long tokenExpiry = 0;

    /**
     * Get metadata for a recording by ISRC
     */
    public SpotifyMetadata getMetadataByIsrc(String isrc) {
        if (isrc == null || isrc.trim().isEmpty()) {
            return null;
        }

        try {
            ensureValidToken();

            String url = SEARCH_URL + "?q=isrc:" + isrc + "&type=track&limit=1";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> tracks = (Map<String, Object>) body.get("tracks");

                if (tracks != null) {
                    java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) tracks
                            .get("items");
                    if (items != null && !items.isEmpty()) {
                        return extractMetadata(items.get(0));
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Spotify lookup failed for ISRC {}: {}", isrc, e.getMessage());
            return null;
        }
    }

    /**
     * Search for tracks by title and artist
     */
    public SpotifyMetadata searchByTitleAndArtist(String title, String artist) {
        if (title == null || title.trim().isEmpty())
            return null;

        try {
            ensureValidToken();

            String query = "track:" + title;
            if (artist != null && !artist.trim().isEmpty()) {
                query += " artist:" + artist;
            }

            String url = SEARCH_URL + "?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "&type=track&limit=1";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> tracks = (Map<String, Object>) body.get("tracks");
                if (tracks != null) {
                    java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) tracks
                            .get("items");
                    if (items != null && !items.isEmpty()) {
                        return extractMetadata(items.get(0));
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Spotify search failed for title '{}': {}", title, e.getMessage());
            return null;
        }
    }

    private SpotifyMetadata extractMetadata(Map<String, Object> track) {
        SpotifyMetadata metadata = new SpotifyMetadata();
        metadata.setTitle((String) track.get("name"));

        Map<String, Object> externalIds = (Map<String, Object>) track.get("external_ids");
        if (externalIds != null) {
            metadata.setIsrc((String) externalIds.get("isrc"));
        }

        metadata.setPopularity((Integer) track.get("popularity"));
        metadata.setDurationMs((Integer) track.get("duration_ms"));

        java.util.List<Map<String, Object>> artists = (java.util.List<Map<String, Object>>) track.get("artists");
        if (artists != null && !artists.isEmpty()) {
            metadata.setArtist((String) artists.get(0).get("name"));
        }

        Map<String, Object> album = (Map<String, Object>) track.get("album");
        if (album != null) {
            metadata.setAlbum((String) album.get("name"));
        }

        log.info("✨ Spotify enrichment: Found '{}' by '{}'", metadata.getTitle(), metadata.getArtist());
        return metadata;
    }

    private void ensureValidToken() {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiry) {
            refreshToken();
        }
    }

    private void refreshToken() {
        try {
            if (clientId == null || clientSecret == null || clientId.contains("your_id")) {
                throw new RuntimeException("Missing Spotify Credentials in application.properties!");
            }

            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            String body = "grant_type=client_credentials";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                accessToken = (String) responseBody.get("access_token");
                Integer expiresIn = (Integer) responseBody.get("expires_in");
                tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000) - 60000;
                log.info("Spotify access token refreshed");
            }
        } catch (Exception e) {
            log.error("Failed to refresh Spotify token: {}", e.getMessage());
            throw new RuntimeException("Spotify Authentication Failed", e);
        }
    }

    @Data
    public static class SpotifyMetadata {
        private String title;
        private String artist;
        private String album;
        private String isrc;
        private Integer popularity;
        private Integer durationMs;
    }
}
