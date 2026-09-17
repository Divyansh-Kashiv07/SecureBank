# ADR-001 — Documentation Authority & Status

**Date:** 2026-09-10
**Status:** Accepted

## Context

The repository shipped with three overlapping documents describing the system:

| Document | Problem |
|---|---|
| `implementation_plan.md` (repo root) | Original *build plan*; some details superseded |
| `SecureBank/OOP_Architecture_Report.md` | Describes a `models/` package and a "HSBC edition" structure that **do not match the actual code** (`core/` package, no `models/`) |
| Root `README.md` | Closest to reality, but had a wrong seeded customer name and plaintext-PIN documentation |

The documents disagree on package layout, class names, and seeded customer
names, which misleads contributors and evaluators.

## Decision

1. **Source code is the single source of truth.** Documentation must describe
   the code as it is (`src/main/java/com/securebank/**`).
2. `OOP_Architecture_Report.md` and `implementation_plan.md` are preserved as
   **historical artifacts**; each carries a status banner pointing readers to
   the authoritative sources.
3. Future structural changes must update the root `README.md` and add a new
   ADR in `SecureBank/docs/adr/` describing the decision and its rationale.

## Consequences

- Readers are no longer sent to a non-existent package layout.
- Documentation drift is caught by review: any PR changing packages or
  wire protocol must touch README + ADRs in the same change-set.
- The viva/exam narrative remains available through the preserved artifacts.
