package com.codemora.fantasy_league.chip;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "squad_chip")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SquadChip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "squad_chip_id")
    private Long id;

    @Column(name = "squad_id", nullable = false)
    private Long squadId;

    @Column(name = "gameweek_id", nullable = false)
    private Long gameweekId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chip_type", nullable = false, length = 16)
    private ChipType chipType;

    @Column(name = "activated_at", nullable = false)
    private LocalDateTime activatedAt;
}
