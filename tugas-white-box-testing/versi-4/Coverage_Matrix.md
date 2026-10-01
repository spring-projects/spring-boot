# Versi 4 — Matriks Coverage

## Persamaan Kuadrat
| TC | a==0 | b==0 | c==0 | D<0 | D==0 | SC | BC |
|---|---|---|---|---|---|---|---|
| Q01 (0,0,0) | T | T | T | - | - | ✓ | T |
| Q02 (0,0,1) | T | T | F | - | - | ✓ | F |
| Q03 (0,2,-4) | T | F | - | - | - | ✓ | F |
| Q04 (1,0,1) | F | - | - | T | - | ✓ | T |
| Q05 (1,-2,1) | F | - | - | F | T | ✓ | T |
| Q06 (1,-3,2) | F | - | - | F | F | ✓ | F |
| Q07 (2,4,1) | F | - | - | F | F | ✓ | F |

Target SC = 100% dan BC = 100%. LC tidak diterapkan karena method tidak mempunyai loop eksplisit.