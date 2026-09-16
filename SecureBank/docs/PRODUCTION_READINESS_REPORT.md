# SecureBank — Production-Readiness Report

**Date:** 2026-09-10 · **Scope:** Phases 0–5, hardening the existing capstone codebase without a rewrite. Every claim below is backed by a command that was actually run and its observed output; the two exceptions are explicitly listed under "Not verified".

---

## 1. What changed (by phase)

| Phase | Change | Key files |
|-------|--------|-----------|
| 0 | Repo hygiene: bundled Maven distribution, preview images, and `data/` files (containing **plaintext demo PINs**) removed from git tracking; doc-reconciliation banner + ADR-001; README factual fixes | `.gitignore`, `README.md`, `docs/adr/ADR-001*` |
| 1 | Test safety net: JUnit 5 harness, configurable data dir (`securebank.data.dir`), ephemeral-port server for integration tests, 65 tests | `pom.xml`, `FileIOHelper`, `BankServer`, `src/test/**` |
| 2 | Correctness: overdraft limit now persists; `appliedAt` restored on loan load; disbursements typed `LOAN_DISBURSEMENT`; **savings minimum-balance (₹1,000) and current-account overdraft actually enforced** in one shared validation path; frozen accounts reject all money movement; remarks sanitized (pipes stripped, 120-char cap) | `Account`, `Transferable`, `Loan`, `LoanProcessor`, `ClientHandler`, `FileIOHelper`, `AccountInactiveException` |
| 3 | Security: per-connection sessions (LOGIN mandatory); ownership checks on every command (IDOR eliminated); salted PBKDF2 PIN hashing (120k iters, constant-time compare, legacy-plaintext auto-upgrade); 5-strikes/5-min login lockout; unified auth errors (no user enumeration); PIN masking in logs; 256-char request cap; `SAVE` server-side only; generic internal errors | `Session` (new), `PasswordHasher` (new), `ClientHandler`, `BankServer`, `Customer` |
| 4 | Reliability: **atomic saves** (temp file + `ATOMIC_MOVE` — a crash mid-write can no longer corrupt data files); save failures throw `IOException` and are logged `CRITICAL` (never silently swallowed, never reported as client errors to avoid double-crediting); transaction logger **drains its queue on stop()** and survives per-write IO errors | `FileIOHelper` (rewritten), `TransactionLogger`, repositories, `LoanProcessor`, `BankServer`, `ClientHandler` |
| 5 | Docs (README security/testing sections, ADR-002, this report, manual GUI script); `mvn verify` gate; **packaged-JAR smoke test** through the real artifact | `README.md`, `docs/**`, `target/smoke/SmokeTest.java` |

## 2. Why

The audit found the architecture sound but the seams broken: no access control (any customer could drain any other customer's account), plaintext PINs committed to git, silent persistence failures that could lose money movements, and non-atomic saves that could destroy data on a crash. Phases 2–4 close those seams; Phase 1's tests exist so every later change is verified, not claimed.

## 3. Tests run and results (evidence)

| Gate | Command | Result |
|------|---------|--------|
| Phase 1 | `mvn test` | 65 tests, 0 failures, 0 errors (surefire XML totals) |
| Phase 2 | `mvn test` | 75 tests, 0 failures, 0 errors |
| Phase 3 | `mvn test` | 90 tests, 0 failures, 0 errors |
| Phase 4 | `mvn test` | 95 tests, 0 failures, 0 errors |
| Final gate | `mvn verify` | exit 0; 95 tests, 0 failures, 0 errors; fat JAR built (1.18 MB) |
| Packaged-JAR smoke | `java -jar target/securebank-1.0-SNAPSHOT.jar 18890` + independent `SmokeTest` client | **ALL 7 CHECKS PASSED** (see §5) |

Suite composition (95): domain rules (deposit/withdraw/transfer, minimum balance, overdraft, frozen accounts, daily limits, EMI, loan lifecycle) · generic `Repository<T>` incl. 8-thread write race · file round-trips · PBKDF2 (salting, uniqueness, legacy, corruption safety) · atomic-write crash safety · logger drain · **protocol-level integration tests against a real TCP server** (mandatory login, full IDOR attack matrix, lockout incl. correct-PIN-while-locked and per-session scope, malformed-request resilience, QUIT, SAVE refusal) · concurrency (8 clients × deposits with zero lost updates, opposite-direction transfers deadlock-free with conserved totals, concurrent withdrawals never breach the floor).

## 4. Browser/GUI verification

Swing cannot be driven by automated tooling (documented constraint from the plan). Verification was done at the strongest available layers — protocol-level integration tests and the packaged-JAR smoke test — and a **10-minute manual GUI script** (`docs/MANUAL_GUI_TEST_SCRIPT.md`) covers the visual layer: login/lockout flows, theme switching, business rules through the forms, IDOR isolation, persistence across restart, and window-resize behavior.

## 5. Packaged-JAR smoke test (the real artifact)

Booted `java -jar target/securebank-1.0-SNAPSHOT.jar 18890` (fresh data dir), then attacked it from an independent compiled client:

```
PASS  unauthenticated command refused            → ERROR|Authentication required. LOGIN first.
PASS  LOGIN with demo PIN accepted (PBKDF2 path) → OK|Divyansh Kashiv|ACC-001001,ACC-001002
PASS  own account balance readable               → OK|25000.00
PASS  IDOR blocked (other customer's account)    → ERROR|Not authorized…
PASS  IDOR blocked (withdrawal attempt)          → ERROR|Not authorized…
PASS  SAVE refused for clients                   → ERROR|Not authorized
PASS  server still healthy after attacks         → OK|25000.00
```

Server log confirms masked login lines (`LOGIN|customerId|****`), single-line logging (a double-log leak found in the first smoke run was fixed and re-verified), and `AUTHORIZATION DENIED` audit entries for the attempted IDOR. Shutdown hook saved all data atomically.

## 6. Security considerations

- **Fixed:** IDOR across all 14 commands; plaintext PIN storage; PIN leakage in logs; user enumeration; unauthenticated `SAVE`; unbounded request size; internal-error detail leakage; double-logging of raw LOGIN lines.
- **Design decisions:** per-connection sessions (zero GUI/protocol break for the single-socket Swing client); PBKDF2 in-JDK (no new dependency); lockout per-session (a spammed connection can't lock out the real customer from another connection). Full rationale in `docs/adr/ADR-002-security-reliability-model.md`.
- **Residual (documented, accepted for scope):** localhost-only TCP without TLS (mitigated by single-machine deployment; v2: TLS or REST+HTTPS); lockout is per-connection (no IP-level rate limit); no idle-session timeout; demo auto-approves loans (explicitly kept for exam demo, documented).

## 7. Performance considerations

No blind optimization was done. Measurements implicit in the suite: 8-client concurrency tests complete in seconds; PBKDF2 at 120k iterations keeps LOGIN ~instant while making offline PIN cracking expensive; atomic saves batch all records per file (no per-record file churn). The N+1-shaped per-transaction full-file auto-save is retained deliberately (demo scale, data-safety over throughput) and flagged as the first thing to change when moving to a database.

## 8. Documentation updated

`README.md` (features, security model, testing matrix, package structure, run instructions, troubleshooting), `docs/adr/ADR-001-documentation-authority.md`, `docs/adr/ADR-002-security-reliability-model.md`, `docs/MANUAL_GUI_TEST_SCRIPT.md`, this report. Runbook: `mvn verify` → `java -jar target/securebank-1.0-SNAPSHOT.jar [port]`.

## 9. Remaining risks

1. **GUI unverified by automation** — mitigated by the manual script; still human-dependent.
2. **Plaintext data files on existing installs** — the committed ones were removed from git; a pre-existing clone's `data/` will auto-upgrade PINs on first login (verified path), but other plaintext artifacts may linger on old machines.
3. **File persistence** — atomic per save, but not transactional across files (accounts+customers+loans save sequentially); a mid-sequence crash could leave repositories consistent individually but not mutually. Acceptable at demo scale; the database migration (README future scope) resolves it.
4. **No TLS** — fine for the single-JVM deployment; must be addressed before any networked deployment.
5. **`Bank`/`Branch` classes remain vestigial** — harmless, kept (documented in ADR-001's authority note) to avoid churn.

## 10. Recommended next step

Commit the work in logical slices (phase 0 hygiene, phase 1 tests, phases 2–4 fixes with their tests, phase 5 docs), then run the manual GUI script once on a clean machine before the exam demo. After that, the highest-value v2 item is the documented session-token + idle-timeout upgrade if the app is ever shown beyond localhost.

## Not verified (explicitly)

- The Swing GUI's rendered behavior (by tooling; manual script provided instead).
- `package.bat`/WiX Windows-installer path (WiX Toolset not installed on this machine; the fat JAR path is verified end-to-end).
