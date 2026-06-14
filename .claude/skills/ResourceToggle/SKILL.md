```markdown
# ResourceToggle Development Patterns

> Auto-generated skill from repository analysis

## Overview
This skill teaches you the core development patterns and conventions used in the ResourceToggle Java codebase. You will learn about file naming, import/export styles, commit practices, and how to write and organize tests. This guide is ideal for contributors looking to maintain consistency and quality in the ResourceToggle project.

## Coding Conventions

### File Naming
- Use **camelCase** for file names.
  - Example: `resourceToggleManager.java`

### Import Style
- Use **relative imports** within the project.
  - Example:
    ```java
    import resourceToggleManager;
    ```

### Export Style
- Use **named exports** for classes and functions.
  - Example:
    ```java
    public class ResourceToggleManager {
        // class implementation
    }
    ```

### Commit Patterns
- Commit messages are **freeform** and do not follow a strict prefix.
- Average commit message length: **~42 characters**.
  - Example:
    ```
    Add toggle logic for resource visibility
    ```

## Workflows

### Adding a New Resource Toggle
**Trigger:** When you need to add a new toggleable resource.
**Command:** `/add-toggle`

1. Create a new Java file using camelCase naming.
2. Implement the toggle logic in a named class.
3. Use relative imports for dependencies.
4. Export the class using a named export.
5. Write a corresponding test file (`*.test.java`).
6. Commit your changes with a descriptive message.

### Updating an Existing Toggle
**Trigger:** When modifying logic for an existing resource toggle.
**Command:** `/update-toggle`

1. Locate the relevant Java file.
2. Update the logic as needed.
3. Ensure all imports remain relative.
4. Update or add test cases in the corresponding test file.
5. Commit with a clear message describing the update.

### Writing Tests
**Trigger:** When adding or updating tests for resource toggles.
**Command:** `/write-test`

1. Create or update a test file matching the `*.test.java` pattern.
2. Write test cases for all public methods and edge cases.
3. Follow the same import/export conventions as production code.
4. Run tests using the project's preferred method (framework unknown).
5. Commit with a message like "Add tests for ResourceToggleManager".

## Testing Patterns

- Test files use the `*.test.java` naming pattern.
  - Example: `resourceToggleManager.test.java`
- Testing framework is **unknown**; follow existing patterns.
- Tests should cover all public methods and edge cases.
- Organize tests in the same directory structure as source files.

## Commands
| Command         | Purpose                                            |
|-----------------|----------------------------------------------------|
| /add-toggle     | Add a new resource toggle                          |
| /update-toggle  | Update logic for an existing resource toggle        |
| /write-test     | Add or update tests for resource toggles           |
```
