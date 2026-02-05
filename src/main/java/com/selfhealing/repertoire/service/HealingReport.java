package com.selfhealing.repertoire.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class HealingReport {
    private int totalOrphans;
    private int healedByDirectMatch;
    private int healedByFuzzyMatch;
    private int remainingOrphans;
    private Map<String, String> details = new HashMap<>();

    public void addDetail(String recording, String result) {
        details.put(recording, result);
    }
}
