# Testing IdeaFlow

[Documentation index](README.md) · [Developer guide](DEVELOPMENT.md) · [Jenkins pipeline](JENKINS.md)

IdeaFlow tests run as an Eclipse/OSGi test bundle inside the same KNIME target platform used to
build the extension. This catches bundle wiring and KNIME runtime problems that a plain Java unit
test runner would miss.

## Running the suite locally

From the repository root:

```text
mvn --batch-mode --no-transfer-progress -f ideaflow-knime/pom.xml clean verify
```

The command compiles the extension, starts the Tycho test runtime, executes all JUnit tests, builds
the feature, and assembles the installable p2 update-site ZIP. Test reports are written below
`ideaflow-knime/IDEAFlow.tests/target/surefire-reports/`.

Jenkins runs this exact reactor command and is the authoritative CI and release pipeline.

## Test layers

The suite is split by responsibility:

- API tests cover validation, immutability, state values, vectors, and evaluation budgets.
- Core tests cover formulas, deterministic randomness, binary encoding, Pareto ranking,
  hypervolume, quality indicators, SHADE memory, migration, and population schedules.
- KNIME contract tests cover problem metadata, every registered factory, icons, table ports,
  optional ports, and settings round trips.
- Node execution tests use in-memory KNIME tables to exercise initialization, evaluation,
  selection, crossover, mutation, elitism, migration, ranking, reference directions, progress
  tracking, run analysis, plots, population traces, front comparison, and loop archive feedback.
- IO tests write temporary IOHprofiler output and verify accounting, escaping, direction handling,
  and path safety.

## Adding a test

Put tests under `ideaflow-knime/IDEAFlow.tests/src` in the same package structure as the code they
cover. Reuse `NodeTestHarness` for node-model lifecycle tests and `TestPopulation` for a small,
metadata-bearing population table. Prefer deterministic seeds and assert behavioral contracts
such as schemas, row counts, state transitions, bounds, NFE values, and clear validation errors.

Whenever a node's ports or settings change, update its factory contract test and add an execution
test for the new behavior. A release is not ready while `clean verify` fails.
