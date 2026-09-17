# SecureBank — Implementation Notes

**Author:** Divyansh Kashiv

This document outlines the architectural decisions and technical reasoning behind SecureBank's core components.

## 1. Concurrency Model (Client-Server)

SecureBank relies on a thread-per-client model for handling TCP connections. When a client connects via `BankServer.java`, a dedicated `ClientHandler` thread is spawned. This enables multiple clients to connect simultaneously without blocking the main server loop.

To prevent race conditions on shared data, the `Account.java` class is heavily synchronized. Both `deposit()` and `withdraw()` are synchronized methods, meaning if two clients attempt to modify the same account simultaneously, the JVM's intrinsic monitor lock ensures the operations happen sequentially.

## 2. File-Based Persistence

Data is stored persistently in plain text files using `java.io.BufferedReader` and `BufferedWriter` (implemented in `FileIOHelper.java`).

While a relational database would provide ACID guarantees natively, the decision to use file persistence was intentional:
- No external dependencies required to run the project.
- Demonstrates deep understanding of Java IO streams.
- Shows how to handle atomic saves manually by writing to a temporary file before using `Files.move()` with `ATOMIC_MOVE`.

## 3. Generic Data Access Layer (`Repository<T>`)

The project uses a generic `Repository<T>` pattern for data access. Instead of writing separate CRUD logic for `AccountRepository` and `CustomerRepository`, a shared HashMap-backed generic class provides type-safe storage, retrieval, and predicate-based search functionality.

## 4. Design System (`ThemeManager.java`)

To avoid the dated appearance of default Swing applications, `ThemeManager.java` implements a custom design token system built on top of FlatLaf.

- **Centralized Colors:** HSL-adjusted color tokens for dark, navy, and neon themes.
- **Micro-interactions:** Custom UI delegates for rounded buttons and text fields.
- **Dynamic Updates:** When switching themes, `SwingUtilities.updateComponentTreeUI()` is called recursively, combined with our custom repainting hooks to ensure instantaneous switching without a restart.

## 5. Knowledge Graph Analytics

I implemented a static analysis tool that parses the project's Abstract Syntax Tree (AST) to generate a force-directed network graph (1,164 nodes, 57 communities). The `graph.json` data is visualized via a custom D3.js frontend (`graph_fancy.html`). This allows new contributors to explore the codebase visually and understand dependencies at a glance.
