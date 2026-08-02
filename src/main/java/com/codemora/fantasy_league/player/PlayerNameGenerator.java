package com.codemora.fantasy_league.player;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Candidate names for admin-generated simulated players (#25) -- a fixed,
 * deterministic list (first x last name combinations) rather than a random
 * name library, mirroring SimulatedTeamNameGenerator's approach. Unlike team
 * names, player names have no uniqueness constraint, so the caller is free to
 * reuse a combination across players.
 */
@Component
public class PlayerNameGenerator {

    private static final List<String> FIRST_NAMES = List.of(
            "Liam", "Noah", "Oliver", "Elijah", "James", "William", "Lucas", "Henry", "Mateo", "Ethan",
            "Kai", "Leo", "Marcus", "Diego", "Youssef", "Kwame", "Rafael", "Idris", "Bruno", "Felix");

    private static final List<String> LAST_NAMES = List.of(
            "Silva", "Johnson", "Garcia", "Mensah", "Rossi", "Kowalski", "Nakamura", "Osei", "Fernandez", "Novak",
            "Andersson", "Dubois", "Hassan", "Costa", "Ivanov", "Okafor", "Larsson", "Moreno", "Haddad", "Petrov");

    public List<String> candidateNames() {
        List<String> names = new ArrayList<>(FIRST_NAMES.size() * LAST_NAMES.size());
        for (String lastName : LAST_NAMES) {
            for (String firstName : FIRST_NAMES) {
                names.add(firstName + " " + lastName);
            }
        }
        return names;
    }
}
