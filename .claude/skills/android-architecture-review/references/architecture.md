# Architecture checklist

- **Dependency rule.** `domain` imports nothing from `android.*`, Retrofit, Room, Hilt or `data`/`presentation`. Violation → `architecture`, medium.
- **ViewModel dependencies.** A ViewModel receives use cases or repository interfaces through its constructor. It never receives or builds `Retrofit`, `OkHttpClient`, a DAO, `Context` or `SharedPreferences`.
- **Repository.** Interface lives in `domain`, implementation in `data`. Mapping DTO → domain model happens in `data`, never in the ViewModel.
- **Use cases.** Exist when they hold business logic or are reused. A pure pass-through use case is acceptable but not required — do not report its absence on trivial CRUD.
- **Business rules location.** Discounts, limits, thresholds, eligibility ("can submit claim", "can checkout") belong to domain models/policies, not to the ViewModel or Composable.
- **Magic numbers** that are business rules (thresholds, fees, percentages) → named constants in domain.
- **DI.** Constructor injection; no service locators or `object` singletons holding state; no Activity `Context` in singletons (leak).
- **Navigation.** ViewModel emits an event/state; it never holds a `NavController`.
- **Duplication.** The same calculation in several loops/places → single domain property (`lineTotal`, `subtotal`).
