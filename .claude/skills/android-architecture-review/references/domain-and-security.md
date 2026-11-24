# Domain & security checklist

## Domain modelling
- **Money as `Double`/`Float`** → domain, high: rounding errors. Use a `Money` value class on minor units (`Long`) or `BigDecimal` with explicit rounding.
- **Primitive obsession**: IDs, policy numbers, promo codes, categories as raw `String`/`Int` → domain, low/medium. Use `value class` / `enum`.
- **Missing invariants**: negative quantities, blank IDs, end date before start date accepted silently → domain, medium. Enforce in `init { require(...) }` or factory.
- **Ambiguous business rule** (e.g. free-shipping threshold before or after discount) → report as domain/low with a question to product, do not guess.

## Security (banking / insurance)
- Secrets in code, `BuildConfig`, resources or logs → security, high.
- Personal data (PESEL, IBAN, policy number, e-mail, phone) logged via `Log`, Timber, Crashlytics custom keys or analytics → security, high.
- Sensitive data in plain `SharedPreferences` / unencrypted files → security, high. Use DataStore/Room + Android Keystore-backed encryption (Tink).
- `android:allowBackup="true"` with sensitive local data, exported components without permission, `WebView` with `setJavaScriptEnabled(true)` + `addJavascriptInterface` on untrusted content → security, high.
- Cleartext traffic, disabled certificate validation, custom `TrustManager` accepting all → security, high.
- Screens with personal data without `FLAG_SECURE` where policy requires it → security, medium.
