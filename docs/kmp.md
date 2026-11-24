# Kotlin Multiplatform — współdzielona domena

Moduł `:domain` jest multiplatformowy: te same reguły zgłoszenia szkody działają w aplikacji Android i mogą być użyte w aplikacji iOS.

| Target | Użycie |
|---|---|
| `jvm` | konsumowany przez moduły Android (`:data`, `:app`) |
| `iosArm64`, `iosSimulatorArm64` | framework `ClaimsDomain` (statyczny) dla aplikacji iOS |

## Co jest współdzielone

- Modele: `Money` (grosze, `Long`), `PolicyNumber`, `Claim`, `ClaimDraft`, typy i statusy.
- Reguły biznesowe: `ClaimRules` — termin zgłoszenia, data w przyszłości, kwota, opis.
- Use case’y: `SubmitClaimUseCase`, `ObserveClaimsUseCase` (coroutines + `Flow`).
- Porty: `ClaimRepository`, `PreferencesRepository` — implementacje są platformowe.

## Decyzje

| Decyzja | Dlaczego |
|---|---|
| `kotlinx-datetime` zamiast `java.time` | `java.time` nie istnieje na iOS; `Clock` i `TimeZone` wstrzykiwane — deterministyczne testy |
| Brak `@Inject` w domenie | `javax.inject` to JVM; na Androidzie use case’y składa `app/di/UseCaseModule` (Hilt), na iOS — konstruktor |
| Porty zamiast `expect/actual` | repozytorium to większa zależność — interfejs + implementacja per platforma jest łatwiejszy do testów niż `expect/actual` |
| Target `jvm`, nie `androidTarget` | domena nie potrzebuje Android SDK; testy uruchamiają się szybko na JVM |

## Komendy

```bash
./gradlew :domain:jvmTest                          # testy na JVM (także w CI)
./gradlew :domain:iosSimulatorArm64Test            # testy na symulatorze iOS (macOS + Xcode)
./gradlew :domain:linkDebugFrameworkIosSimulatorArm64   # framework dla Xcode
```

## Użycie w Swift

Framework importuje się w Xcode jak każdy inny (`import ClaimsDomain`). Dwie rzeczy warto wiedzieć przed wystawieniem API dla iOS:

- `suspend fun` i `Flow` z Kotlina są w Swift niewygodne bez dodatkowego narzędzia — rozwiązuje to SKIE (async/await, `AsyncSequence`).
- Value classy (`Money`, `PolicyNumber`) nie przechodzą do Swift jako osobne typy, więc dla iOS lepiej wystawić cienką fasadę z typami prostymi.
