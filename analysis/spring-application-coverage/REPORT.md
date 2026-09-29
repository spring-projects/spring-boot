# Coverage analysis report

## 1. Method and scope

The paper defines statement coverage (SC) as executing every statement at least
once, branch coverage (BC) as exercising both outcomes of every decision, and
loop coverage (LC) as considering loop behavior rather than merely counting the
loop body. Its GA encodes CFG nodes, uses a longest node/edge-weight path as a
target, selects tournaments of size two, uses crossover probability 0.5 and
mutation probability 0.01, and stops at fitness 13 for its TriClass experiment
(paper pp. 3-5, Tables 1-3). Those numerical results are experiment-specific;
they are not claims about SpringApplication.

The object under study is only `SpringApplication.java`. This is a source-level
CFG analysis, not a claim that every framework collaborator has been instrumented.
In particular, an `if` condition containing `&&` or `||` is treated as one
source decision in the primary catalog. A tool using short-circuit condition
coverage may report more atomic outcomes.

## 2. Source inventory

The cited line numbers refer to the current file in this checkout. The main
startup CFG is `run(String...)` (lines 306-349):

* `R1` line 307: shutdown-hook registration decision.
* `R2` lines 320-328: startup-info logging decision inside the protected path.
* `R3` lines 330-332: startup failure exception edge.
* `R4` lines 334-336: context-running decision.
* `R5` lines 338-340: ready-phase failure exception edge.

The environment/property slice adds:

* `E1` line 362: custom-environment conversion decision.
* `E2` lines 374-377: custom factory fallback and null-environment fallback.
* `P1` line 499: conversion-service setup.
* `P2` line 515: default properties present.
* `P3` line 518: command-line properties enabled and arguments non-empty.
* `P4` line 521: an existing command-line property source is present.

Context preparation and runner behavior add:

* `C1` line 387 and `C2` line 389: bean-factory type decisions.
* `C3` line 396, `C4` line 405, `C5` line 408, `C6` line 412:
  startup/context options.
* `C7` line 422 and `C8` line 425: generated-artifact initializer paths.
* `C9` line 436 and `C10` line 442: native-image and shutdown-hook paths.
* `I1` lines 617-623: initializer loop and its assertion edges.
* `L1` lines 771-773: runner-name loop.
* `L2` lines 844-849: exception-reporter loop and early return.
* `L3` lines 1715-1722: keep-alive loop; normal sleep and interrupt exit are
  distinct loop outcomes.

This inventory deliberately names source decisions instead of treating every
executed bytecode instruction as a statement. For example, line 377's ternary
has two outcomes, while line 374's `&&` can have short-circuit sub-outcomes.

## 3. Cyclomatic complexity and basis paths

For a connected CFG, cyclomatic complexity is `M = E - N + 2`, equivalently
`M = D + 1` when `D` is the number of binary source decisions in the selected
slice. A basis-path set has `M` linearly independent paths; it is not the set of
all paths, which is generally exponential and may be infinite with loops.

### Bounded `run` slice

For `run(String...)`, counting the five source decisions `R1`-`R5` gives
`M_run = 6`. One basis set (edge labels are source outcomes) is:

| Path | Edges/outcomes | Purpose |
| --- | --- | --- |
| `B1` | R1=T, R2=T, R3=F, R4=T, R5=F | ordinary startup with startup logging |
| `B2` | R1=F, R2=T, R3=F, R4=T, R5=F | no shutdown-hook registration |
| `B3` | R1=T, R2=F, R3=F, R4=T, R5=F | startup logging disabled |
| `B4` | R1=T, R2=T, R3=T | prepare/refresh/call-runner failure |
| `B5` | R1=T, R2=T, R3=F, R4=F | context is not running; no ready event |
| `B6` | R1=T, R2=T, R3=F, R4=T, R5=T | ready listener failure |

`B4` and `B6` are exception paths. `B5` is feasible only with a test double or
an application context that stops before the ready check; a normal successful
refresh usually makes `R4=F` infeasible. The `R5=T` path is feasible when the
ready listener throws. The `R3=T` and `R5=T` edges are not interchangeable:
they invoke failure handling with different listener arguments.

The `run` slice is intentionally not presented as the complexity of the whole
class. The class has many methods, nested classes, exception edges, callbacks,
and loops; a single whole-class number would be difficult to reproduce and
would obscure which entry point the paths describe. Apply `M = D + 1` to any
additional method after fixing its entry/exit and decision granularity.

### Property-source slice

For `configurePropertySources` (lines 508-530), the four decisions `P2`-`P4`
plus the command-line conjunction give a source-decision complexity of `M=5`.
The independent obligations include: no defaults/defaults; command-line
disabled or empty; command-line enabled with no existing source; and command
line enabled with an existing source. The final application-info source
statement is common to all paths.

## 4. Branch obligations and existing test mapping

BC requires both outcomes of each catalogued decision, not merely that the
line was executed. A practical obligation matrix for the current class is:

| Decision family | True/positive obligation | False/negative obligation | Feasibility note |
| --- | --- | --- | --- |
| `R1`, `R2`, `R4` | option enabled / context running | option disabled / context not running | `R4=F` often needs a double |
| `R3`, `R5` | exception edge | protected work completes | listener and startup failures differ |
| `E1`, `E2` | conversion/fallback taken | custom environment or factory succeeds | depends on injected factory |
| `P2`-`P4` | properties/source exists | absent or disabled | directly configurable in tests |
| `C1`-`C10` | matching subtype/option | alternate subtype/option | some depend on AOT/native runtime |
| `I1`, `L1`, `L2` | non-empty loop and body edge | empty collection | reporter early return is another branch |
| `L3` | interrupted sleep exits | sleep continues | requires controlled thread/event |

`core/spring-boot/src/test/java/org/springframework/boot/SpringApplicationTests.java`
contains broad behavioral tests for invalid sources, banners, environments,
listeners, runners, failure handling, and exit behavior. Those tests are
valuable evidence, but their aggregate pass count must not be called SC or BC:
one test can execute a statement without exercising its alternate outcome, and
framework callbacks can hide decisions in collaborators. The report therefore
requires recording outcome pairs (`T` and `F`) per decision, separately from
statement execution counts.

## 5. Loop coverage obligations

For each loop, the minimal educational obligations are zero iterations, one
iteration, and multiple iterations where feasible:

* `I1` initializer loop (line 617): empty initializer set, one initializer,
  and at least two initializers; assertion failure is a separate exceptional
  edge.
* `L1` runner-name loop (line 771): no runners, one runner, and multiple
  runners (including ordering).
* `L2` exception-reporters loop (line 844): no reporters, one reporter that
  returns true (early exit), and multiple reporters where the first returns
  false and a later reporter handles the failure.
* `L3` keep-alive loop (line 1715): at least one sleep iteration followed by
  interruption. The conceptual zero-iteration case (interrupt before the first
  sleep) is scheduler-sensitive and should be tested only with a controlled
  thread harness.

Loop bounds are not “source execution counts”: running `L1` twice does not prove
that `L2` has both reporter outcomes.

## 6. Feasible paths, infeasible paths, and limits

Paths involving AOT-generated initializers, native-image detection, CRaC, or
specific application-context implementations are environment-dependent. Some
combinations are infeasible because mutually exclusive configuration values
select one factory or because the framework guarantees a refreshed context is
running. Exception paths may also require mocks or fault injection rather than
ordinary application inputs. Such paths should be marked infeasible or
uncovered-with-reason, not silently counted as covered.

The paper's Euclidean CFG edge weights and GA fitness target are useful teaching
devices for prioritizing paths, but this repository does not claim to reproduce
the paper's 13-node TriClass target, 92-second CodeCover run, or two-test GA
result. SpringApplication's dynamic framework callbacks, reflection, classpath
conditions, and external resources exceed the paper's small example model.

The quadratic sources and executable tests in this directory are a separate
teaching fixture: they demonstrate how alternative control-flow structures can
implement the same mathematical requirement while preserving correct roots.
They are not production replacements for SpringApplication.
