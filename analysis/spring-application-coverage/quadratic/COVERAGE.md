# Quadratic fixture coverage

All three versions implement the same input partition:

| Case | Decision outcomes | Expected result |
| --- | --- | --- |
| `a != 0`, discriminant `> 0` | quadratic / two real roots | two distinct roots |
| `a != 0`, discriminant `= 0` | quadratic / repeated root | equal roots |
| `a != 0`, discriminant `< 0` | quadratic / no real roots | two `NaN` values |
| `a == 0`, `b != 0` | linear fallback | one root and second `NaN` |
| `a == 0`, `b == 0` | degenerate equation | `IllegalArgumentException` |

`QuadraticEquationVariantsTest` executes every row for V1, V2, and V3. V3 also
gets cancellation-sensitive (`x² + 10^16x + 1`) and large-scaled coefficient
cases. V3 scales its coefficients before computing the discriminant and uses
`q = -0.5 * (b + copySign(sqrt(discriminant), b))`, obtaining the other root as
`c / q`; this avoids subtracting nearly equal values in the small-root
calculation. Finite coefficients are assumed. As with ordinary `double`
arithmetic, extreme ratios may still underflow or produce an unrepresentable
root.

The Java tests are **functional input-partition/outcome tests**. They verify
expected results for representative decision cases; they do not instrument
source execution, count executed statements, or calculate statement/branch
coverage percentages. Thus they do not establish measured SC or BC, nor any
percentage coverage. The SpringApplication report is likewise a static,
source-derived obligation/path analysis and contains no instrumented coverage
measurement. The quadratic implementations contain no loops, so loop coverage
is not applicable. Compile and run the tests using the commands in the parent
[`README.md`](../README.md).
