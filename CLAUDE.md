# Project rules for AI agents

Android app "Zgłoszenie szkody" (claim reporting) + AI engineering toolkit around it.

## Architecture (non-negotiable)
- Modules: `:app` (presentation, Compose), `:domain` (pure Kotlin), `:data` (Retrofit/Room/DataStore), `:shared` (KMP, optional).
- `:domain` has no Android, Retrofit, Room or Hilt imports. Repository interfaces live in `:domain`.
- ViewModels: constructor-injected use cases/repositories + `CoroutineDispatcher`; expose one `StateFlow<UiState>`; one-off events via `Channel`.
- Money is `Money` (value class on grosze, `Long`). Never `Double`.
- Business rules (eligibility, limits, thresholds) live in `:domain`.
- Formatting (currency, dates) only in presentation.

## Testing
- Every use case and ViewModel change comes with unit tests: JUnit, MockK, Turbine, `StandardTestDispatcher`.
- Prefer fakes over mocks for repositories.
- UI: Compose Test Rule for critical flows (submit claim).

## Data & security
- Only synthetic data in code and tests (`app/src/test/fixtures`). Real customer data never enters the repo or a prompt.
- Do not read paths in `security/restricted-paths.txt`. Follow `security/AI-usage-policy.md`.
- No secrets in code, `BuildConfig` or logs. No personal data in logs/analytics.

## Working style
- Small, reviewable changes. Run `./gradlew test lint` (or the `android-dev` MCP tools) before declaring done.
- Before committing Android code, run the `android-reviewer` agent and fix high-severity findings.
- Do not add dependencies without stating why; verify the artifact exists.
