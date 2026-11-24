---
name: android-architecture-review
description: Review Kotlin/Android code (ViewModels, repositories, use cases, Compose screens) for Clean Architecture, coroutine, state-management, Compose, security and testability defects. Use when asked to review a diff, a pull request, or Android files, or before committing Android changes.
---

# Android architecture review

Review Android/Kotlin code the way a senior reviewer on a banking/insurance app would:
find defects that matter, point to exact lines, propose the fix. Do not comment on style
that ktlint/detekt already enforce.

## Process

1. **Scope.** Review only the files/diff you were given. If given a PR, run `git diff origin/main...HEAD -- '*.kt'`.
2. **Read the project rules** in `CLAUDE.md` — they override generic advice.
3. **Check each file against the checklists** (load only the ones relevant to the file):
   - `references/architecture.md` — layers, dependency rule, repositories, use cases, DI
   - `references/coroutines.md` — scopes, dispatchers, cancellation, Flow, error handling
   - `references/state-and-compose.md` — UiState, one-off events, recomposition, side effects
   - `references/domain-and-security.md` — money, value objects, invariants, sensitive data
4. **Verify before reporting.** For every candidate finding, re-read the exact line. Drop it if
   you cannot point to a concrete failure scenario. Prefer 8 real findings over 20 guesses.
5. **If tools are available** (android-dev MCP server), run `gradle_test` / `gradle_lint` on the
   affected module and include failures as findings.

## Output

Return **only** JSON, no prose before or after:

```json
{
  "findings": [
    {
      "file": "app/src/main/java/.../CartViewModel.kt",
      "line": 46,
      "category": "concurrency",
      "severity": "high",
      "title": "GlobalScope outlives the screen",
      "why": "Work continues after the ViewModel is cleared; no cancellation, leaks references.",
      "fix": "Use viewModelScope.launch and inject the dispatcher."
    }
  ],
  "summary": "One or two sentences on the overall state of the change."
}
```

- `category` ∈ `architecture | state | concurrency | domain | error_handling | testability | compose | security | duplication`
- `severity` ∈ `high | medium | low` — high = bug, data loss, security or leak; medium = architecture/testability debt; low = maintainability.
- `line` = the line where the defect is visible (first line for multi-line defects).
- One finding per defect. If the same defect repeats in several places, report the first line and list the others in `why`.
