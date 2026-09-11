# ADR-002 — Security & Reliability Model (2026-09-10)

## Status
Accepted

## Context
The original SecureBank codebase had no authentication or authorization model:
after an optional `LOGIN`, every protocol command trusted the account number
supplied by the client. Any connected customer could read and **drain any other
customer's account** (classic IDOR). PINs were stored and logged in plaintext,
the `SAVE` command was callable by any client, and persistence used
non-atomic direct writes that could destroy data on a crash or disk-full error.

The project is an academic capstone with exam-room demo constraints: it must
run from a single JAR on classroom machines, keep demo PINs usable
(`1234`, `5678`, `9012`), and remain understandable for viva evaluation.

## Decision

### 1. Per-connection sessions (no tokens in v1)
Each TCP connection gets a `Session`. `LOGIN` authenticates the **connection**;
every subsequent command is authorized against the session customer. A session
binds on first successful login and cannot switch customers.

Rationale: the GUI holds exactly one persistent socket, so per-connection
sessions give complete IDOR protection with zero protocol break and zero GUI
changes. Signed tokens with idle timeouts are the documented v2 step.

### 2. Salted PBKDF2-HMAC-SHA256 PIN hashing (no new dependency)
PINs are stored as `pbkdf2:iterations:saltB64:hashB64` (120,000 iterations,
16-byte random salt, 256-bit key, constant-time comparison via
`MessageDigest.isEqual`). Legacy plaintext records are detected by the missing
prefix and transparently upgraded on the next successful login. Demo PINs are
hashed at seed time, so no plaintext PIN ever reaches a data file.

Rationale: PBKDF2 is built into the JDK — no dependency, no viva-unfriendly
complexity. The self-describing stored format lets the iteration count rise
later without migration. 120k iterations is below OWASP's web-password
guidance by design: the input is a 4-digit PIN over a localhost socket, and
logins must stay instant on classroom machines; it is still ~10,000× costlier
than a naive hash.

### 3. Login lockout and unified auth errors
5 consecutive login failures lock the **session** for 5 minutes; even the
correct PIN is refused while locked. Unknown customer and wrong PIN return the
identical message, eliminating user enumeration. Lockout is per-connection, so
a spoofed failure cannot deny service to the real customer from another
connection.

### 4. Ownership checks on every command
`requireOwnedAccount()` verifies the target account exists AND belongs to the
session customer before `BALANCE`, `DEPOSIT`, `WITHDRAW`, `TRANSFER` (source),
`HISTORY`, `ACCOUNT_INFO`, `INTEREST`, and `LOAN_APPLY`. `LOAN_STATUS`,
`ACCOUNTS`, and `LOAN_APPLY` (customerId) additionally refuse other customers'
IDs. `CREATE_ACCOUNT` ignores any customerId parameter — accounts are created
only for the session customer. `SAVE` is server-side only.

### 5. Protocol hardening
Requests are capped at 256 chars; LOGIN lines are masked in logs (`****`);
unexpected handler errors return a generic `ERROR|Internal server error` with
details only in server logs; `QUIT` closes cleanly.

### 6. Atomic, honest persistence
All saves write a temp file in the same directory and `ATOMIC_MOVE` it over the
target — a crash mid-write can never corrupt an existing data file. Save
methods throw `IOException`; the server logs `CRITICAL` on auto-save failure
but does NOT report an error to the client for an already-executed in-memory
operation (reporting failure would trigger client retries and double-credit
money). The transaction logger drains its queue on `stop()`, so a transaction
logged just before shutdown is never lost.

### 7. Business-rule correctness consolidated in the domain
`Account.validateWithdrawal()` is the single shared rule set for `withdraw()`
and `transferTo()`: daily limit → available balance (CurrentAccount may draw
into its overdraft limit) → savings minimum balance (₹1,000 must remain).
Frozen accounts reject all money movement via the checked
`AccountInactiveException`. Loan disbursements record `LOAN_DISBURSEMENT`
(instead of a plain `DEPOSIT`) so history and reports can distinguish them.

## Consequences
- The raw-socket demo clients in tests must `LOGIN` before operating —
  intentional and asserted by `ProtocolIntegrationTest.requiresLogin`.
- `data/customers.dat` now contains hashes; fresh seed data is created on first
  run. Existing committed plaintext data files were removed from git in Phase 0.
- Overdraft and minimum-balance rules changed runtime behavior (previously
  unenforced); tests were updated deliberately and the change is viva-explainable.
- Verified by the 95-test suite, including protocol-level attack scenarios
  (`SecurityIntegrationTest`, IDOR scenarios in `ProtocolIntegrationTest`).

## Documented next steps (v2)
- Signed session tokens with idle timeout and server-side revocation
- Rate limiting by source IP in addition to per-session lockout
- Interest crediting via the existing `INTEREST` transaction type
- Migration from file persistence to a database with transactional guarantees
