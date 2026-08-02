package com.codemora.fantasy_league.season;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Candidate names for admin-generated simulated teams (#19/#25) -- a fixed,
 * deterministic list (prefix x suffix combinations) rather than random
 * generation, so it's straightforward to test and the caller controls
 * uniqueness by skipping names that already exist.
 */
@Component
public class SimulatedTeamNameGenerator {

    private static final List<String> PREFIXES = List.of(
            "North", "South", "East", "West", "Central", "Royal", "Port", "Kings", "Fort", "Lake",
            "River", "Hill", "Vale", "Forest", "Bridge", "Grand", "Old", "New", "Union", "Crescent");

    private static final List<String> SUFFIXES = List.of(
            "United", "City", "Rovers", "Athletic", "Town", "Wanderers", "Albion", "Rangers", "Villa", "County");

    public List<String> candidateNames() {
        List<String> names = new ArrayList<>(PREFIXES.size() * SUFFIXES.size());
        for (String suffix : SUFFIXES) {
            for (String prefix : PREFIXES) {
                names.add(prefix + " " + suffix);
            }
        }
        return names;
    }
}
