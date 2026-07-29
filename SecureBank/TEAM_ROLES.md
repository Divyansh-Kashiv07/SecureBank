# 🏦 SecureBank — Team Roles & Work-Split Guide

> Maps NIET's official 5 rubric roles to specific files/packages, so each team member can own and explain their piece in the viva.

---

## Role 1: Team Lead & Business Analyst

**Owner:** [Name]

**Responsibilities:**
- Overall project coordination and delivery timeline
- Requirements gathering and problem statement analysis (SDG 8 alignment)
- Domain model design (what classes are needed and why)

**Files to Own & Explain:**
| Package | Files | Key Concepts |
|---------|-------|--------------|
| `com.securebank.core` | `Bank.java`, `Branch.java`, `Customer.java` | Association, Aggregation, OOP relationships |
| `com.securebank.core` | `Transferable.java` (interface) | Interface design, capability contracts |
| Root | `README.md` | Project documentation, architecture overview |
| Root | `TEAM_ROLES.md` | Team coordination |

**Viva Talking Points:**
- Explain the UML relationships: Association (Customer–Account), Aggregation (Bank–Branch), Composition (Account–Transaction)
- Explain why we chose a client-server architecture over a standalone app
- Describe the SDG 8 alignment (decent work and economic growth — financial inclusion)

---

## Role 2: System Architect & Design Lead

**Owner:** [Name]

**Responsibilities:**
- Package structure design and class diagram
- Client-server architecture decisions
- GUI layout and UX design decisions

**Files to Own & Explain:**
| Package | Files | Key Concepts |
|---------|-------|--------------|
| `com.securebank.core` | `Account.java` (abstract) | Abstract class, `synchronized`, method overloading |
| `com.securebank.core` | `SavingsAccount.java`, `CurrentAccount.java` | Generalization, method overriding |
| `com.securebank.gui` | `SecureBankApp.java` | CardLayout, sidebar navigation, MVC-ish structure |
| `com.securebank.gui.components` | `SidebarPanel.java`, `CardPanel.java` | Custom Swing components, Java2D painting |

**Viva Talking Points:**
- Explain the `Account` abstract class design: why `calculateInterest()` is abstract, how overloading (`deposit(double)` vs `deposit(double, String)`) works
- Explain why `synchronized` was placed on `deposit()` and `withdraw()` (race condition prevention)
- Walk through the GUI architecture: why CardLayout, why sidebar navigation

---

## Role 3: Technical Development Lead

**Owner:** [Name]

**Responsibilities:**
- Core business logic implementation
- Server-side socket programming
- Thread management and concurrency

**Files to Own & Explain:**
| Package | Files | Key Concepts |
|---------|-------|--------------|
| `com.securebank.server` | `BankServer.java` | ServerSocket, TCP, accept loop, thread spawning |
| `com.securebank.server` | `ClientHandler.java` | `implements Runnable`, request/response protocol |
| `com.securebank.client` | `BankClient.java` | Socket client, synchronized communication |
| `com.securebank.main` | `Main.java` | CLI args, daemon threads, EDT, startup sequence |

**Viva Talking Points:**
- Explain TCP socket programming: ServerSocket vs Socket, why TCP (not UDP) for banking
- Explain the protocol: `COMMAND|param1|param2` request format
- Explain threading: why each client gets its own thread, what `setDaemon(true)` does
- Explain the `synchronized` keyword and how it prevents race conditions in `Account`
- Walk through the sequence diagram (see README)

---

## Role 4: Java Technology & Integration Lead

**Owner:** [Name]

**Responsibilities:**
- Generics implementation (`Repository<T>`)
- Collections usage (HashMap, ArrayList, TreeMap)
- Lambda expressions and Streams
- File I/O persistence

**Files to Own & Explain:**
| Package | Files | Key Concepts |
|---------|-------|--------------|
| `com.securebank.repository` | `Repository.java` | Generics (`<T>`), HashMap, `Predicate<T>`, lambdas |
| `com.securebank.repository` | `AccountRepository.java`, `CustomerRepository.java` | Generic repo usage, file persistence |
| `com.securebank.utils` | `FileIOHelper.java` | BufferedReader/BufferedWriter, Character Streams |
| `com.securebank.transactions` | `TransactionLogger.java` | Daemon thread, BlockingQueue, async I/O |
| `com.securebank.utils` | `IDGenerator.java` | AtomicInteger, thread-safe counters |

**Viva Talking Points:**
- **Generics deep-dive**: What `<T>` means, how type erasure works, why `Repository<Account>` prevents you from inserting a `Customer`
- **Lambda expressions**: Explain `Predicate<T>`, how `acc -> acc.getBalance() > 10000` is shorthand for an anonymous class
- **Character Streams vs Byte Streams**: Why we used BufferedReader/Writer, what buffering does
- **BlockingQueue**: How the producer-consumer pattern works in TransactionLogger

---

## Role 5: QA/Documentation/Presentation Lead

**Owner:** [Name]

**Responsibilities:**
- Testing and bug identification
- Custom exception design and error handling
- Report generation and receipts
- Final presentation preparation

**Files to Own & Explain:**
| Package | Files | Key Concepts |
|---------|-------|--------------|
| `com.securebank.exceptions` | All 5 exception classes | Checked exceptions, try-catch, throw/throws |
| `com.securebank.utils` | `ReceiptGenerator.java` | StringBuilder, formatted output |
| `com.securebank.loans` | `Loan.java`, `LoanStatus.java`, `LoanProcessor.java` | EMI calculation, state machine |
| `com.securebank.transactions` | `Transaction.java`, `TransactionType.java` | Immutability, enums |
| `com.securebank.gui` | `TransactionHistoryPanel.java` | JTable, filtering, jagged arrays |
| `com.securebank.gui.components` | `NotificationPanel.java`, `StyledButton.java` | Toast notifications, UI polish |

**Viva Talking Points:**
- **Exception handling**: Checked vs Unchecked, why our banking exceptions extend `Exception` (not `RuntimeException`), demonstrate `try-catch-finally`, multiple catch blocks, nested try
- **StringBuilder**: Why it's faster than `+` concatenation for building receipts
- **Testing**: How to test concurrent access (open 2 clients, deposit simultaneously)
- **Jagged arrays**: The monthly summary grid in TransactionHistoryPanel

---

## Shared Viva Topics (Everyone Must Know)

These topics may be asked to ANY team member:

1. **Why `synchronized`?** Prevents race conditions when multiple clients modify the same account balance simultaneously.

2. **Why TCP (not UDP)?** Banking data MUST be reliably delivered, in order, with error detection. TCP guarantees all three.

3. **What is the Generic Repository?** A reusable `Repository<T>` class that works with any entity type — `Repository<Account>`, `Repository<Customer>`, etc. — using Java Generics.

4. **What is a daemon thread?** A background service thread that the JVM will terminate automatically when all non-daemon threads finish. Our TransactionLogger uses it.

5. **What is FlatLaf?** A modern Look-and-Feel library that replaces Swing's default grey appearance with a flat, IDE-style design.
