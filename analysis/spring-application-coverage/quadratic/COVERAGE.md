# Quadratic fixture coverage

All three versions implement the same input partition:

| Case | Decision outcomes | Expected result |
| --- | --- | --- |
| `a != 0`, discriminant `> 0` | quadratic / two real roots | two distinct roots |
| `a != 0`, discriminant `= 0` | quadratic / repeated root | equal roots |
| `a != 0`, discriminant `< 0` | quadratic / no real roots | two `NaN` values |
| `a == 0`, `b != 0` | linear fallback | one root and second `NaN` |
| `a == 0`, `b == 0` | degenerate equation | `IllegalArgumentException` |

`QuadraticEquationVariantsTest` executes every row for V1, V2, and V3. This is
an outcome-oriented fixture, not a claim of instrumentation-derived percentage
coverage. It deliberately keeps statement execution, branch outcomes, and
loop coverage distinct; these implementations contain no loops, so loop
coverage is not applicable. Compile and run it using the commands in the parent
[`README.md`](../README.md).
