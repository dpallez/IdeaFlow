# IdeaFlow

*Interactive Design of Evolutionary Algorithms as a workFlow*

[IdeaFlow](https://github.com/dpallez/IdeaFlow) is a [KNIME Analytics Platform](https://www.knime.com/knime-analytics-platform) extension for composing population-based optimization workflows from reusable nodes.

<img width="1920" height="894" alt="IdeaFlow in KNIME" src="https://github.com/user-attachments/assets/12039d62-e7bc-4c02-b2cc-8acfd10023bd" />

## Documentation

| Topic | Documentation |
| --- | --- |
| Documentation map | [Documentation index](docs/README.md) |
| Concepts and workflow architecture | [Overview](docs/OVERVIEW.md) |
| Installing KNIME | [KNIME installation](docs/KNIME_INSTALLATION.md) |
| Installing and verifying IdeaFlow | [IdeaFlow installation](docs/INSTALLATION.md) |
| Complete catalogue of the 19 IdeaFlow nodes | [Node reference](docs/NODES.md) |
| Built-in optimization problems | [Optimization problems](docs/OPTIMIZATION_PROBLEMS.md) |
| Building an optimization workflow | [Workflow tutorial](docs/WORKFLOW_TUTORIAL.md) |
| Reusing workflows | [Sub-workflows](docs/SUBWORKFLOWS.md) |
| Architecture and contribution workflow | [Developer guide](docs/DEVELOPMENT.md) |
| Tests and Jenkins | [Testing](docs/TESTING.md) and [Jenkins](docs/JENKINS.md) |
| Workflow quality and common problems | [Best practices](docs/BEST_PRACTICES.md), [Troubleshooting](docs/TROUBLESHOOTING.md), and [FAQ](docs/FAQ.md) |

## Supported workflow families

IdeaFlow exposes configurable operators rather than hiding complete algorithms behind a single node. The included example workflows demonstrate:

- genetic algorithms;
- Differential Evolution (DE) and jDE;
- SHADE and L-SHADE with archive feedback;
- NSGA-II, NSGA-III, and GDE3;
- homogeneous multi-population island models.

These workflows use the same reusable selection, crossover, mutation, evaluation, elitism, migration, and analysis nodes. See the [example workflow catalogue](ideaflow-knime/examples/EXAMPLES.md).

## Built-in optimization problems

The Evaluation node supports `ACKLEY`, `SPHERE`, `ROSENBROCK`, `RASTRIGIN`, `GRIEWANK`, `ONEMAX`, `ZDT1`, `ZDT2`, `ZDT3`, and `DTLZ2`. It can also evaluate formulas or validate objective and constraint columns produced by other KNIME nodes.

Definitions and references are collected in [Optimization Problems and Benchmarks](docs/OPTIMIZATION_PROBLEMS.md).

## Main capabilities

- reproducible runs with bounded float, integer, binary, and binary-encoded variables;
- single-, multi-, and many-objective problems with optional constraints;
- built-in benchmark, mathematical-formula, and upstream-column evaluation;
- strict number-of-function-evaluations (NFE) loop budgets and objective-target stopping;
- GA, DE, jDE, SHADE, L-SHADE, NSGA-II, NSGA-III, and GDE3 operator configurations;
- DE archive feedback across loop iterations;
- Pareto ranking, reference directions, reference-front indicators, and population migration;
- progress histories, repeated-run analysis, convergence and ECDF views, population traces, and IOHprofiler export;
- public Java API and SPI packages for reusable state and strategy extensions.

## Project status

The current version is IdeaFlow 0.1.0 Alpha 2. The extension is built as an Eclipse p2 update site and currently targets KNIME Analytics Platform 5.11 with Java 21. Jenkins is the authoritative build and release pipeline.

## Acknowledgements

The authors gratefully acknowledge the support of DS4H (Digital Systems for Humans), whose funding contributed to the research, development, and documentation of the IdeaFlow project.
