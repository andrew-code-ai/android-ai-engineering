# android-ai-engineering

Praktyczny zestaw narzędzi AI dla zespołu Android w sektorze finansowo-ubezpieczeniowym: agent do code review, bezpieczne użycie AI z maskowaniem danych, własny serwer MCP i audyt kosztu i jakości wywołań modelu.

Motyw przewodni: aplikacja **„Zgłoszenie szkody”** (Kotlin, Jetpack Compose, MVVM + Clean Architecture). Moduł aplikacji powstaje w `/app`, a reszta repo już działa.

## Co tu jest

| Kompetencja | Gdzie | Status |
|---|---|---|
| Skill i agent AI wspierający development | [`.claude/skills/android-architecture-review`](.claude/skills/android-architecture-review), [`.claude/agents/android-reviewer.md`](.claude/agents/android-reviewer.md), [`CLAUDE.md`](CLAUDE.md) | ✅ |
| Wdrożenie agenta w CI (review każdego PR) | [`.github/workflows/ai-review.yml`](.github/workflows/ai-review.yml) | ✅ — wymaga sekretu `ANTHROPIC_API_KEY` |
| Audyt kosztu i jakości: routing, prompt caching, batching | [`eval/`](eval) | ✅ pipeline + symulacja, ⏳ realne liczby po uruchomieniu z kluczem |
| Bezpieczne użycie AI: klasy danych, dobór narzędzi, maskowanie | [`security/AI-usage-policy.md`](security/AI-usage-policy.md), [`security/masking`](security/masking), [`.claude/settings.json`](.claude/settings.json) | ✅ z testami |
| Integracje MCP | [`.mcp.json`](.mcp.json), własny serwer [`mcp/android-dev-mcp`](mcp/android-dev-mcp), [`docs/mcp.md`](docs/mcp.md) | ✅ z testami |
| bash / PowerShell | [`scripts/`](scripts) | ✅ |
| Aplikacja demo (Compose, Hilt, Room, testy) | `/app` | ⏳ |

## Szybki start

```bash
./scripts/setup.sh                 # macOS/Linux   (Windows: ./scripts/setup.ps1)
./scripts/run-eval.sh --dry-run    # symulacja audytu bez kosztów
export ANTHROPIC_API_KEY=...       # potem realny audyt:
./scripts/run-eval.sh
claude                             # Claude Code wczyta CLAUDE.md, skill, agenta, hooki i MCP
```

## 1. Agent do code review

- **Skill** `android-architecture-review`: proces przeglądu, 4 checklisty (architektura, coroutines, stan i Compose, domena i bezpieczeństwo) i ścisły format wyniku w JSON. Checklisty ładowane są tylko wtedy, gdy dotyczą pliku.
- **Subagent** `android-reviewer`: ma uprawnienia tylko do odczytu, sam uruchamia testy przez MCP i zwraca znaleziska z plikiem, linią, kategorią, wagą i propozycją poprawki.
- **CLAUDE.md**: reguły projektu, których agent musi przestrzegać (warstwy, `Money` zamiast `Double`, testy, dane syntetyczne).
- **CI**: GitHub Action uruchamia Claude Code headless na zmienionych plikach `.kt` i publikuje komentarz w PR z tabelą uwag i kosztem przeglądu.

## 2. Bezpieczne użycie AI

[Polityka](security/AI-usage-policy.md) definiuje 4 klasy danych (A–D), macierz „narzędzie × klasa danych” i standard maskowania. Polityka jest **egzekwowana kodem**:

- Hook `UserPromptSubmit` blokuje prompt zawierający PESEL, NIP, IBAN, kartę, e-mail, telefon, numer polisy lub szkody albo numer rejestracyjny. Użytkownik dostaje wersję zamaskowaną do ponownego wysłania.
- Identyfikatory z sumą kontrolną (PESEL, NIP, IBAN mod 97, Luhn) są walidowane, więc 11-cyfrowy numer builda nie zostanie uznany za PESEL.
- Hook `PreToolUse` i `permissions.deny` blokują agentowi odczyt eksportów danych, keystore i `local.properties`.
- Log decyzji zapisuje tylko **typy** wykrytych danych, nigdy wartości.

```
> Napraw walidację dla klienta 44051401359
Zablokowano: prompt zawiera dane wrażliwe (PESEL)…
Wersja zamaskowana do ponownego wysłania:
Napraw walidację dla klienta [PESEL]
```

## 3. MCP

Własny serwer **android-dev-mcp** (TypeScript, oficjalne SDK MCP) daje agentowi `gradle_test`, `gradle_lint`, `gradle_task`, `adb_devices`, `logcat_tail` i `run_ui_tests`:

- nie ma dowolnego shella, komendy są budowane z walidowanych parametrów i uruchamiane przez `execFile`;
- próby wstrzyknięcia (`:app; rm -rf /`) są odrzucane, co sprawdza test;
- dane osobowe w logcat są maskowane, zanim trafią do modelu;
- wyjście jest przycinane do 200 linii, co chroni kontekst i koszt.

Konfiguracja projektu w [`.mcp.json`](.mcp.json) obejmuje android-dev, GitHub i Figma. Szczegóły: [docs/mcp.md](docs/mcp.md).

## 4. Audyt kosztu i jakości

Zestaw ewaluacyjny w [`eval/dataset`](eval/dataset) to 4 pliki Kotlin z udokumentowanymi wadami:

| Przypadek | Wady |
|---|---|
| `CartViewModel` (God ViewModel) | 19 |
| `ClaimRepository` (bezpieczeństwo, coroutines) | 10 |
| `ClaimListScreen` (Compose) | 8 |
| czysty `SubmitClaimUseCase` | 0 — mierzy fałszywe alarmy |

Każdy wariant jest mierzony na tym samym zbiorze:

| Wariant | Hipoteza |
|---|---|
| `baseline` | punkt odniesienia |
| `caching` | stały prefiks (skill + checklisty) w cache, kolejne pliki płacą głównie za swój kod |
| `routing` | tani model odsiewa pliki niewymagające przeglądu, drogi model tylko dla ryzykownych |
| `batch` | Message Batches API dla nocnego przeglądu całego repo, wynik asynchronicznie, niższa stawka |

Mierzone są recall, precision, trafność kategorii, fałszywe alarmy na czystym pliku, tokeny (wejście, zapis i odczyt cache, wyjście) i koszt. Wynik trafia do [`eval/results/report.md`](eval/results).

> Wyniki: _do uzupełnienia po pierwszym realnym uruchomieniu._

## Struktura

```
.claude/            skill, subagent, hooki (settings.json)
.github/workflows/  AI review w CI + testy bezpieczeństwa
security/           polityka AI, maskowanie danych + testy
mcp/android-dev-mcp własny serwer MCP + testy
eval/               zestaw testowy, warianty, scoring, raport
scripts/            setup i eval (bash + PowerShell)
docs/               dokumentacja MCP
app/                aplikacja demo „Zgłoszenie szkody” (w budowie)
```

Wszystkie dane w repo są syntetyczne.
