# Audyt kosztu i jakości — android-reviewer

Data: 2026-11-24 19:01

| Wariant | Recall | Precision | Kategoria trafna | FP na czystym pliku | Tokeny in / cache W / cache R / out | Koszt USD | vs baseline |
|---|---|---|---|---|---|---|---|
| baseline | 46% | 74% | 88% | 1 | 18009 / 0 / 0 / 11054 | 0.1466 | — |
| caching | 46% | 81% | 94% | 1 | 4885 / 3281 / 9843 / 10817 | 0.1271 | -13% |
| routing | 19% | 78% | 100% | 1 | 9379 / 0 / 9843 / 8351 | 0.0871 | -41% |
| batch | 43% | 64% | 94% | 3 | 4885 / 3281 / 9843 / 11167 | 0.0653 | -55% |

- `baseline` błędy: {'cart_viewmodel': "JSONDecodeError: Expecting ',' delimiter: line 74 column 6 (char 5355)"}

- `caching` błędy: {'cart_viewmodel': "JSONDecodeError: Expecting ',' delimiter: line 92 column 6 (char 6920)"}

- `routing` błędy: {'cart_viewmodel': "JSONDecodeError: Expecting ',' delimiter: line 92 column 6 (char 6685)", 'claim_repository': 'JSONDecodeError: Expecting value: line 1 column 1 (char 0)'}

- `batch` błędy: {'cart_viewmodel': "parse: Expecting ',' delimiter: line 110 column 6 (char 7135)"}

## Pominięte wady (wariant baseline)

- **cart_viewmodel**: recall 0%, pominięte: C01, C02, C03, C04, C05, C06, C07, C08, C09, C10, C11, C12, C13, C14, C15, C16, C17, C18, C19
- **claim_repository**: recall 90%, pominięte: R10
- **claim_screen**: recall 100%, pominięte: —
- **submit_claim_usecase**: recall 100%, pominięte: —
