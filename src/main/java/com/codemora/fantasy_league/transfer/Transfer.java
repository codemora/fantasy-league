package com.codemora.fantasy_league.transfer;

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
@Table(name = "transfer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long id;

    @Column(name = "squad_id", nullable = false)
    private Long squadId;

    @Column(name = "gameweek_id", nullable = false)
    private Long gameweekId;

    @Column(name = "player_out_id", nullable = false)
    private Long playerOutId;

    @Column(name = "player_in_id", nullable = false)
    private Long playerInId;

    /** 0 when covered by a free transfer, otherwise TransferService.TRANSFER_POINTS_COST. */
    @Column(name = "points_cost", nullable = false)
    private Integer pointsCost;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
