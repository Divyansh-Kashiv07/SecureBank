# Contributing to SecureBank

Thank you for your interest in contributing to SecureBank!

This project is actively maintained and developed by **Divyansh Kashiv** as a capstone project for the "Object Oriented Techniques using Java" course at NIET Greater Noida.

## How to Contribute

While this is primarily an academic project, I welcome discussions, issue reports, and pull requests that improve the codebase or demonstrate advanced Java patterns.

### 1. Reporting Bugs
If you find a bug, please open an issue in the repository. Provide steps to reproduce, expected behavior, and actual behavior.

### 2. Suggesting Enhancements
Have an idea for a new feature (e.g., adding PDF receipt generation)? Open an issue detailing your proposal so we can discuss its feasibility.

### 3. Submitting Pull Requests
1. Fork the repository.
2. Create a new branch for your feature (`git checkout -b feature/your-feature-name`).
3. Make your changes and write clear commit messages.
4. Run all JUnit tests (`mvn test`) to ensure nothing is broken.
5. Push to your branch and open a Pull Request.

## Code Style Guidelines
- **Java conventions:** Follow standard Oracle/Sun Java naming conventions.
- **Concurrency:** Any modifications to shared data structures must be properly synchronized or use `java.util.concurrent` collections.
- **UI Code:** Long-running tasks triggered from the Swing GUI must use a `SwingWorker` to avoid blocking the Event Dispatch Thread (EDT).

For any questions about the architecture, please review the `docs/IMPLEMENTATION_NOTES.md` file.

— Divyansh Kashiv
