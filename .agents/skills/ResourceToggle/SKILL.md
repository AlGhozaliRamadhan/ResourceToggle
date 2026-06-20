```markdown
# ResourceToggle Development Patterns

> Auto-generated skill from repository analysis

## Overview
This skill teaches the core development patterns and conventions used in the ResourceToggle TypeScript codebase. You'll learn about file naming, import/export styles, commit message habits, and how to write and run tests. This guide is ideal for contributors aiming for consistency and maintainability in a TypeScript project without a framework.

## Coding Conventions

### File Naming
- Use **camelCase** for file names.
  - Example: `resourceToggle.ts`, `toggleManager.ts`

### Imports
- Use **relative imports** for referencing modules.
  - Example:
    ```typescript
    import { toggleResource } from './toggleResource';
    ```

### Exports
- Prefer **named exports** over default exports.
  - Example:
    ```typescript
    // In toggleResource.ts
    export function toggleResource() { ... }
    ```

### Commit Messages
- Freeform style, no strict prefixes.
- Average commit message length: ~71 characters.
  - Example:  
    ```
    Add toggle functionality for resource panel
    ```

## Workflows

### Adding a New Feature
**Trigger:** When implementing new functionality.
**Command:** `/add-feature`

1. Create a new file using camelCase naming.
2. Write your feature using TypeScript.
3. Use relative imports to include dependencies.
4. Export functions or variables using named exports.
5. Write or update corresponding test files (`*.test.ts`).
6. Commit your changes with a clear, descriptive message.

### Refactoring Existing Code
**Trigger:** When improving or restructuring code.
**Command:** `/refactor`

1. Identify code to refactor.
2. Update file names to camelCase if needed.
3. Ensure all imports remain relative.
4. Use named exports throughout.
5. Update or add tests to reflect changes.
6. Commit changes with a concise message.

### Writing Tests
**Trigger:** When adding or updating tests.
**Command:** `/write-test`

1. Create a test file with the pattern `*.test.ts`.
2. Write tests for your functions or modules.
3. Use the same import/export conventions as in source files.
4. Run tests using the project's test runner (framework unknown; check project scripts).
5. Commit your test files with a descriptive message.

## Testing Patterns

- Test files follow the pattern: `*.test.ts`
- Testing framework is not specified; check for test scripts or dependencies.
- Tests should import modules using relative paths and named imports.
  - Example:
    ```typescript
    import { toggleResource } from './toggleResource';

    // Example test
    test('toggles resource state', () => {
      // test implementation
    });
    ```

## Commands
| Command      | Purpose                                   |
|--------------|-------------------------------------------|
| /add-feature | Scaffold and implement a new feature      |
| /refactor    | Refactor existing code for improvements   |
| /write-test  | Create and update test files              |
```
