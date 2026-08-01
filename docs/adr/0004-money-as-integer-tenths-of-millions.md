# 0004: Money as integer tenths-of-millions

## Status
Accepted

## Context
Several fields represent money: `Season.startingBudget`, `FantasySquad.bankBalance`, `SquadPlayer.purchasePrice`. Fantasy-sports pricing is conventionally shown with one decimal place (e.g. £7.5m), which invites representing it as a floating-point or `BigDecimal` currency value. Floating-point money is a well-known source of rounding bugs (budget checks that are off by a fraction of a cent compounding across 15 squad players and every transfer); `BigDecimal` avoids that but adds ceremony (scale/rounding-mode handling) that isn't needed here since the domain only ever needs one decimal place of precision.

## Decision
Store all money fields as `int`, denominated in tenths of a million — so `1000` means £100.0m and `75` means £7.5m. All budget/price arithmetic (sum a squad's purchase prices, compare against `bankBalance`) is then plain integer arithmetic with no rounding concerns. The API/UI layer divides by 10 and formats with one decimal place for display.

## Consequences
- Budget and transfer-cost checks (`FantasySquad.isValid()`, transfer validation in #29/#31) are exact integer comparisons — no floating-point drift possible.
- Every layer that touches a money field (API request/response DTOs, any future price-change logic) needs to remember the ×10 convention; a single documented conversion point (entity ↔ DTO mapping) is where that should live, rather than scattering `/ 10.0` across the codebase.
- If a future requirement needs finer-grained pricing (e.g. tenths of a decimal), this representation would need to change; nothing in the current model requires that.
