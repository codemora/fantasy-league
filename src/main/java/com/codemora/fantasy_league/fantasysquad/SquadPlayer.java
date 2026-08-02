package com.codemora.fantasy_league.fantasysquad;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "squad_player")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SquadPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "squad_player_id")
    private Long id;

    @Column(name = "squad_id", nullable = false)
    private Long squadId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "purchase_price", nullable = false)
    private Integer purchasePrice;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;
}
