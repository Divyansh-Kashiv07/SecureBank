# SecureBank — Manual GUI Test Script (One Page)

The automated suite (95 tests) covers the domain, protocol, and server. The Swing
GUI cannot be driven by automated tooling, so verify it by hand with this script.
**Time: ~10 minutes.** Run `mvn verify` first if the JAR is missing.

**Start:** `java -jar target/securebank-1.0-SNAPSHOT.jar` (default port 8888)

**Demo logins:** CUSTOMER-1 / 1234 · CUSTOMER-2 / 5678 · CUSTOMER-3 / 9012

---

## 1. Login screen
| # | Step | Expected |
|---|------|----------|
| 1.1 | Launch the app, leave fields empty, click Sign In | Warning toast, no crash |
| 1.2 | Login as CUSTOMER-1 / **wrong PIN** `0000` | Generic error "Invalid customer ID or PIN." — no hint whether the ID exists |
| 1.3 | Type a wrong PIN **5 times** | 5th attempt reports lockout; even the correct PIN is refused for ~5 minutes (per connection) |
| 1.4 | Launch a **second** app instance, login CUSTOMER-1 / 1234 there | Second instance logs in fine — lockout is per-connection, not global |
| 1.5 | Login CUSTOMER-1 / 1234 (first instance, new connection after lockout expiry or fresh instance) | Success → Dashboard with name, accounts, balances |

## 2. Dashboard & accounts
| # | Step | Expected |
|---|------|----------|
| 2.1 | Observe Dashboard | Savings + Current cards for CUSTOMER-1 (₹25,000 / ₹50,000 seeded), recent transactions, chart renders |
| 2.2 | Open Accounts panel | Both accounts listed with type, interest rate (Savings 4%, Current 1%) |
| 2.3 | Switch theme (Settings) and font scale | UI updates immediately, no layout breakage |

## 3. Money movement (business rules)
| # | Step | Expected |
|---|------|----------|
| 3.1 | Deposit ₹1,000 to Savings | Balance +1,000; success toast; entry appears in history as **Deposit** |
| 3.2 | Withdraw ₹4,500 from Savings (balance 26,000) | Refused: minimum balance ₹1,000 must remain; balance unchanged |
| 3.3 | Withdraw ₹25,000 from Savings | Refused if it would breach ₹1,000 floor or the ₹50,000 daily limit — read the message |
| 3.4 | Withdraw from **Current** up to balance+10,000 | Allowed into overdraft (negative balance shown); beyond overdraft refused |
| 3.5 | remarks: enter text containing `\|` in deposit remarks | Stored/sanitized — history still renders, no broken rows |
| 3.6 | Transfer ₹500 Savings → own Current | Both balances update; history shows Fund Transfer (Out)/(In) |

## 4. Isolation (IDOR) — via UI where visible
| # | Step | Expected |
|---|------|----------|
| 4.1 | As CUSTOMER-1, note the account list | Only ACC-001001/001002 visible — no other customer's accounts anywhere |
| 4.2 | Loan panel as CUSTOMER-3: apply ₹50,000 / 12 months | Approved (demo auto-approval) and disbursed; balance +50,000; history type **Loan Disbursement** |
| 4.3 | Loan panel again on same account | Refused — one active loan per account |
| 4.4 | History filter/search | Filters work; LOAN_DISBURSEMENT rows render correctly |

## 5. Persistence & shutdown
| # | Step | Expected |
|---|------|----------|
| 5.1 | Make a deposit, then close the window (X) | Console shows "Saving all data…" then "Server stopped"; logger footer written |
| 5.2 | Relaunch, login, check history | The deposit is still there (atomic save + load) |
| 5.3 | Inspect `data/customers.dat` | PIN fields start with `pbkdf2:…` — no plaintext PINs |
| 5.4 | Inspect `data/transaction_log.txt` | Last logged transactions present (logger drained on shutdown) |

## 6. Responsive / window behavior
| # | Step | Expected |
|---|------|----------|
| 6.1 | Resize window small → large | Sidebar and cards reflow; no clipped text or overlapping panels |
| 6.2 | Tab through the login form | Focus ring visible on each control; Enter submits |
| 6.3 | Trigger a toast during a transfer, then click elsewhere | Toast dismisses or times out; no dead click zones |

**Pass criterion:** every row behaves as described, console shows no stack traces.
Failures → note the step number and the console output.
