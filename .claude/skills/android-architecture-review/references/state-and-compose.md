# State & Compose checklist

## ViewModel state
- **Mutable state exposed publicly** (`val items: MutableList`, public `var`, public `MutableStateFlow`) → state, high: no single source of truth.
- **Loose state fields** (`isLoading`, `errorText`, `totalText`…) instead of one immutable `UiState` exposed as `StateFlow` → state, medium.
- **Impossible states**: several booleans that can be true together → model with sealed interface.
- **Pre-formatted strings in state** (`"Razem: 19.99 zł"`) → state, medium: keep typed values, format in UI (locale, accessibility, tests).
- **In-place mutation of entities** (`item.quantity = …`) → state, high; entities are immutable, change via `copy`.
- **One-off events** (navigation, snackbar) stored in `StateFlow` → re-fire after rotation. Use `Channel(BUFFERED).receiveAsFlow()` or explicit consumption.

## Compose
- Reading fast-changing state (scroll offset, animation) in composition instead of layout/draw lambdas (`Modifier.offset { }`, `graphicsLayer { }`) → compose, medium.
- `LazyColumn`/`LazyRow` items without stable `key` when items can be reordered/edited → compose, medium.
- Unstable parameters (`List`, classes from non-Compose modules, new lambdas each recomposition) on hot composables → compose, low (mention strong skipping if the project uses Kotlin 2.0+).
- Heavy work (sorting, filtering, formatting lists) directly in composable body without `remember`/`derivedStateOf` → compose, medium.
- `LaunchedEffect(Unit)` that uses values which can change → stale captures; use proper keys or `rememberUpdatedState`.
- ViewModel or repository passed deep into child composables → pass state + lambdas (state hoisting); keeps previews and tests simple.
- Missing `contentDescription` on actionable icons, touch targets < 48dp → compose (accessibility), low.
