# Coroutines & Flow checklist

- **`GlobalScope`** anywhere in app code → high. Use `viewModelScope` or an injected application scope.
- **Hard-coded dispatchers** (`Dispatchers.IO/Main/Default`) in classes under test → testability, medium. Inject `CoroutineDispatcher`.
- **Swallowed cancellation.** `catch (e: Exception)` / `catch (e: Throwable)` around suspend calls without rethrowing `CancellationException` → error_handling, medium.
- **Generic catch with UI string** in logic layers → error_handling: type the error (sealed result / domain exception), map to text in presentation.
- **Callbacks instead of suspend/Flow** in new Kotlin APIs (`onDone: (Boolean) -> Unit`) → architecture, medium.
- **`withContext` inside `flow {}`** → use `flowOn`. Collecting in UI without lifecycle (`launchWhenStarted`, `collectAsState` on Android) → prefer `repeatOnLifecycle` / `collectAsStateWithLifecycle`.
- **Shared mutable state** updated from several coroutines with `value = value.copy(...)` → use `update {}`; `synchronized` around suspend code → use `Mutex`.
- **`stateIn` / `shareIn`** without `SharingStarted.WhileSubscribed(5_000)` for UI state → low.
- **Fire-and-forget work that must survive the screen** (upload, sync) in `viewModelScope` → should be WorkManager.
- **`runBlocking`** on the main thread → high.
