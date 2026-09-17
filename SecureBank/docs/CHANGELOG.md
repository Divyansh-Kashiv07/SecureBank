# Changelog

All notable changes to the SecureBank project will be documented in this file.

## [v1.1.0] - 2026-09-17

**Author:** Divyansh Kashiv

### Added
- **Beneficiaries System:** Added `BeneficiariesPanel.java` to allow customers to save and manage trusted payees securely. Features server-side validation and responsive SwingWorker-based loading.
- **Internationalization (i18n):** Implemented `AppLanguage.java` with a fast HashMap-backed translation engine supporting English and Hindi.
- **ThemeManager Engine:** Introduced a centralized design token system (`ThemeManager.java`) supporting Neon, Navy Blue, and Darker Black themes with dynamic font scaling.
- **Service Layer Refactoring:** Added `BankService.java` to decouple core business logic from TCP socket protocol handlers.
- **Interactive Knowledge Graph:** Extracted a 1,164-node AST graph visualized with a custom D3.js force-directed HTML viewer (`graph_fancy.html`).
- **Security Lockout:** Added a configurable failed-attempt lockout system to the login panel with countdown feedback.
- **Obsidian Vault Export:** Support for converting codebase knowledge graphs into Obsidian-compatible Markdown files.

### Changed
- UI completely overhauled using FlatLaf and custom Java2D rounded components.
- Server architecture hardened with atomic file saves and rigorous lock ordering on fund transfers to prevent deadlocks.
- Refined transaction logging to ensure daemon threads gracefully drain queues on shutdown.

## [v1.0.0] - Initial Release

### Added
- Core TCP client-server architecture with per-connection `ClientHandler` threads.
- Thread-safe `Account` model with synchronized `deposit` and `withdraw` methods.
- Generic `Repository<T>` pattern for type-safe data access.
- File-based persistence using `BufferedReader`/`BufferedWriter`.
- Base Swing GUI with CardLayout navigation (Dashboard, Transfer, History, Loans, Reports).
