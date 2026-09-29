# SpringApplication statement, branch, path, and loop analysis

This directory is an educational, reproducible application of Bahaweres et al.,
**“Analysis of Statement Branch and Loop Coverage in Software Testing With Genetic
Algorithm”**, EECSI 2017, to exactly one production class:

`core/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java`

The paper's TriClass results are not projected onto Spring Boot. The report
describes the paper's definitions and GA idea, then derives obligations from the
current source and identifies feasible and infeasible paths.

## Contents

* [`REPORT.md`](REPORT.md) - source-grounded CFG slices, cyclomatic complexity,
  basis paths, branch obligations, loop obligations, and limits.
* [`quadratic/QuadraticEquationV1.java`](quadratic/QuadraticEquationV1.java) -
  nested-decision version.
* [`quadratic/QuadraticEquationV2.java`](quadratic/QuadraticEquationV2.java) -
  guard-clause version.
* [`quadratic/QuadraticEquationV3.java`](quadratic/QuadraticEquationV3.java) -
  scaled-discriminant, cancellation-resistant roots with sign-classification
  and `switch`.
* [`quadratic/QuadraticEquationProgram.java`](quadratic/QuadraticEquationProgram.java) -
  runnable command-line entry point selecting V1, V2, or V3.
* [`quadratic/QuadraticEquationVariantsTest.java`](quadratic/QuadraticEquationVariantsTest.java) -
  dependency-free executable tests shared by all three versions.
* [`quadratic/COVERAGE.md`](quadratic/COVERAGE.md) - fixture coverage matrix.
* [`tools/verify_source_anchors.py`](tools/verify_source_anchors.py) - checks
  that the report's cited source anchors still exist.

## Reproduce

From the repository root, with JDK 17 or newer and Python 3:

```powershell
python analysis\spring-application-coverage\tools\verify_source_anchors.py
New-Item -ItemType Directory -Force analysis\spring-application-coverage\quadratic\out | Out-Null
javac -d analysis\spring-application-coverage\quadratic\out analysis\spring-application-coverage\quadratic\QuadraticEquationV1.java analysis\spring-application-coverage\quadratic\QuadraticEquationV2.java analysis\spring-application-coverage\quadratic\QuadraticEquationV3.java analysis\spring-application-coverage\quadratic\QuadraticEquationProgram.java analysis\spring-application-coverage\quadratic\QuadraticEquationVariantsTest.java
java -cp analysis\spring-application-coverage\quadratic\out QuadraticEquationVariantsTest
java -cp analysis\spring-application-coverage\quadratic\out QuadraticEquationProgram V2 1 -3 2
```

The Java test command prints one passing line per variant and fails with a
non-zero exit status on any wrong root, classification, or degenerate-input
behavior. The generated `quadratic\out` directory is disposable and is not a
source deliverable.
