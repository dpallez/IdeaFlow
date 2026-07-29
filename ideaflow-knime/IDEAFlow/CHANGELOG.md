# Changelog

## 0.1.0-SNAPSHOT

Initial experimental snapshot of the IdeaFlow KNIME extension.

### Available in the KNIME node repository

- Define a single reproducible optimization run with bounded float, integer, binary, or natural-binary-encoded
  variables; one or more minimize/maximize objectives; optional constraints; an evaluation budget; and optional
  hypervolume reference values.
- Create deterministic initial populations and carry versioned optimizer state and problem metadata through KNIME
  tables.
- Evaluate candidates with built-in benchmark functions, validated mathematical formulas, or numeric result columns
  produced by upstream KNIME nodes. Formula cards are generated from Problem Setup and clearly separated into
  objectives and constraints. Built-in benchmarks expose only constraint formulas, so their objective equations
  cannot be replaced accidentally. Evaluation also decodes encoded variables,
  calculates constraint violations, advances the number of function evaluations (NFE), and emits
  evaluation-history events.
- Build strictly NFE-bounded optimization loops with optional objective-target stopping, accumulated progress history,
  and an internally maintained, population-bounded replaced-parent archive for SHADE/L-SHADE donor sampling. A loop
  stops before a complete next generation when that generation would exceed the evaluation budget.
- Select parents with tournament, random, or Differential Evolution donor selection.
- Apply SBX, uniform, one-point, arithmetic, DE binomial, and DE exponential crossover.
- Apply polynomial, Gaussian, random-reset, bit-flip, DE/rand/1, DE/best/1, current-to-best/1, and
  current-to-pbest/1 mutation, with bounds repair and fixed, jDE, or canonical SHADE success-history parameters.
  SHADE memory is population-wide and uses fitness-improvement-weighted Lehmer means for F and arithmetic means for CR.
- Select survivors using ordinary elitism, pairwise DE competition, NSGA-II, NSGA-III, or GDE3; L-SHADE-style
  linear population reduction is available through the Elitism node.
- Rank constraint-aware Pareto solutions, generate Das-Dennis reference directions, and compare an approximation
  with a reference front using GD, IGD, IGD+, epsilon, and spacing.
- Exchange candidates between multiple populations with ring, deterministic-random, or all-to-all migration at a
  configurable generation interval, without importing a source island's adaptive optimizer memory.
- Track scalar and Pareto progress, record population traces, summarize repeated runs, and display convergence and
  final-fitness ECDF plots.
- Export event histories to standard IOHprofiler metadata, improvement, and optional complete-data files. Scalar
  selection and direction are validated, and an escaped IdeaFlow companion file can retain additional source
  columns.

### Extension contracts and algorithm support

- Public problem, candidate, run-state, event, and custom IdeaFlow state-cell contracts.
- Public strategy interfaces with Java service discovery for bounds repair, dominance, ranking, migration topology,
  quality indicators, and state codecs.
- Reusable optimization kernels supporting GA, NSGA-II, NSGA-III, DE, GDE3, jDE, SHADE, and L-SHADE workflows.
- Modern KNIME node dialogs for every registered node that requires configuration.
- Strict current-snapshot settings and progress-table contracts; obsolete pre-release setting aliases and inferred
  progress identity fields are not retained.
- Automated OSGi regression tests cover DE competition, canonical SHADE memory updates, migration cadence, strict
  generation-level evaluation-budget reservation, built-in/formula evaluation result ownership, and IOHprofiler
  improvement-log correctness, metadata, validation, and sidecar escaping.

### Snapshot limitations

- The extension is experimental and is not yet published on a public KNIME update site.
- Continuous-integration and release workflows are not included yet.
- Example workflows are not bundled with this snapshot.
- CMA-ES, particle swarm optimization, ant-colony optimization, and an integrated surrogate model are not provided.
