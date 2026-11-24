# Integracje MCP

Konfiguracja projektu: [`.mcp.json`](../.mcp.json) — Claude Code wczytuje ją automatycznie po otwarciu repo (przy pierwszym użyciu pyta o zgodę na serwery projektowe).

| Serwer | Typ | Po co | Uwagi |
|---|---|---|---|
| `android-dev` | stdio, własny ([`mcp/android-dev-mcp`](../mcp/android-dev-mcp)) | agent sam uruchamia lint/testy, sprawdza urządzenia i czyta logcat | tylko komendy z allow-listy, bez shella, maskowanie danych w logach |
| `github` | HTTP (zdalny serwer GitHub) | issues, PR, review, CI | token w zmiennej `GITHUB_TOKEN`, nigdy w pliku |
| `figma` | HTTP (Figma Dev Mode, lokalnie) | pobieranie specyfikacji ekranów i tokenów designu do implementacji w Compose | wymaga włączonego Dev Mode MCP w aplikacji Figma |

## android-dev-mcp — narzędzia

| Narzędzie | Co robi |
|---|---|
| `gradle_task` | `lint`, `test`, `assembleDebug`, `detekt`, `ktlintCheck`, `connectedAndroidTest` dla projektu lub modułu |
| `gradle_test`, `gradle_lint` | skróty do najczęstszych zadań |
| `adb_devices` | `adb devices -l` |
| `logcat_tail` | ostatnie ≤500 linii logcat z filtrem tagu i priorytetu, **z zamaskowanymi danymi osobowymi** |
| `run_ui_tests` | testy instrumentalne (Espresso / Compose Test Rule) |

### Model bezpieczeństwa
- Agent nie dostaje dowolnego shella: komendy są budowane z walidowanych parametrów ([`policy.ts`](../mcp/android-dev-mcp/src/policy.ts)) i uruchamiane przez `execFile`, bez interpretera powłoki.
- Nazwy modułów, tagi i numery seryjne walidowane wyrażeniami regularnymi — próba wstrzyknięcia (`:app; rm -rf /`) jest odrzucana (test w `policy.test.ts`).
- Limit czasu (domyślnie 10 min dla Gradle) i limit wyjścia (ostatnie 200 linii) — chroni kontekst modelu i koszt.
- Brak narzędzi do publikacji (Google Play) i podpisywania — zgodnie z AI-usage-policy.md.

## Uruchomienie

```bash
cd mcp/android-dev-mcp && npm install && npm run build && npm test
claude mcp list        # w katalogu repo — powinien pokazać android-dev
```
