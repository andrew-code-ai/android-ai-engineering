# Zasady bezpiecznego wykorzystania AI w zespole mobilnym

Dokument dla zespołu tworzącego aplikację mobilną w sektorze ubezpieczeniowym/finansowym.
Cel: korzystać z AI w całym SDLC, nie wypuszczając poza organizację danych, które nie mogą jej opuścić.

## 1. Klasy danych

| Klasa | Przykłady | Ryzyko wycieku |
|---|---|---|
| **A — publiczne** | dokumentacja Androida, kod open source, publiczne API, treść ze sklepu Google Play | brak |
| **B — wewnętrzne** | kod źródłowy aplikacji, architektura, nazwy endpointów, zadania z JIRA bez danych klientów | średnie: własność intelektualna, wiedza o systemie |
| **C — poufne** | sekrety (klucze API, keystore, tokeny), konfiguracja bezpieczeństwa, raporty z testów penetracyjnych, logi produkcyjne | wysokie: bezpośredni wektor ataku |
| **D — dane klientów** | PESEL, imię i nazwisko, adres, IBAN, numer polisy i szkody, dane pojazdu, dokumentacja medyczna szkody, zdjęcia | krytyczne: RODO, tajemnica ubezpieczeniowa |

## 2. Dobór narzędzia do klasy danych

| Narzędzie | A | B | C | D |
|---|---|---|---|---|
| Publiczny czat AI (konto prywatne) | ✅ | ❌ | ❌ | ❌ |
| Narzędzie AI w planie firmowym (umowa, brak trenowania na danych, ustalona retencja) — np. Claude Code / API na koncie organizacji | ✅ | ✅ | ❌ | ❌ |
| Model uruchamiany w infrastrukturze organizacji (on-prem / prywatna chmura) | ✅ | ✅ | ⚠️ tylko za zgodą security | ⚠️ tylko po zamaskowaniu i za zgodą IOD |
| Jakiekolwiek narzędzie bez umowy powierzenia | ✅ | ❌ | ❌ | ❌ |

Reguła: **klasa C nigdy nie trafia do modelu** — sekret przekazany do AI uznajemy za skompromitowany i rotujemy.
**Klasa D trafia do modelu wyłącznie zamaskowana**, a w pracy deweloperskiej używamy danych syntetycznych.

## 3. Standard maskowania

| Typ | Wykrywanie | Zamiennik |
|---|---|---|
| PESEL | 11 cyfr + suma kontrolna + poprawny miesiąc | `[PESEL]` |
| NIP | 10 cyfr + suma kontrolna | `[NIP]` |
| IBAN | format + kontrola mod 97 | `[IBAN]` |
| Karta płatnicza | 13–19 cyfr + algorytm Luhna | `[CARD]` |
| E-mail | wzorzec adresu | `[EMAIL]` |
| Telefon | 9 cyfr, opcjonalnie +48 | `[PHONE]` |
| Numer polisy / szkody | wzorzec z katalogu danych organizacji | `[POLICY_NO]`, `[CLAIM_NO]` |
| Nr rejestracyjny pojazdu | wzorzec polskich tablic | `[REG_PLATE]` |

Walidacja sum kontrolnych ogranicza fałszywe alarmy (np. 11-cyfrowy numer builda nie jest PESEL-em).
Imion i nazwisk nie da się niezawodnie wykryć wzorcem — w kodzie i testach używamy wyłącznie danych syntetycznych, a dane z produkcji nie trafiają do repozytorium.

## 4. Jak to egzekwujemy (a nie tylko deklarujemy)

1. **Hook `UserPromptSubmit`** (`security/masking/hook.py`) — blokuje prompt z danymi klasy D i podaje wersję zamaskowaną do ponownego wysłania.
2. **Hook `PreToolUse`** — blokuje agentowi odczyt ścieżek z `security/restricted-paths.txt` (eksporty danych, keystore, `local.properties`).
3. **`permissions.deny`** w `.claude/settings.json` — druga, niezależna warstwa blokady odczytu.
4. **Log decyzji** (`security/logs/decisions.jsonl`) — zapisuje tylko typ wykrytych danych, nigdy wartość. Podstawa do audytu.
5. **Sekrety** wyłącznie w zmiennych środowiskowych / GitHub Secrets; `.env`, keystore i `local.properties` w `.gitignore`.
6. **Testy** (`security/masking/test_masking.py`) uruchamiane w CI — zmiana reguł maskowania nie przejdzie bez testów.

## 5. Zasady pracy z wynikiem AI

- Kod od AI przechodzi to samo code review co kod człowieka; autor PR odpowiada za każdą linię.
- AI nie dostaje uprawnień do publikacji (Google Play, produkcyjne API) ani do sekretów CI.
- Zależności zaproponowane przez AI weryfikujemy (czy istnieją, licencja, utrzymanie) — modele potrafią wymyślać nazwy bibliotek.
- Incydent (dane D lub C wysłane do modelu) zgłaszamy od razu do security / IOD.
