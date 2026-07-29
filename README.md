# IdeaFlow
*Interactive Design of Evolutionary Algorithms as a workFlow*

[IdeaFlow](https://github.com/dpallez/IdeaFlow) is a [KNIME Analytics Platform](https://www.knime.com/knime-analytics-platform) extension for composing population-based optimization workflows from reusable nodes.

## Documentation

| Topic | Documentation |
| --- | --- |
| Documentation map | [Documentation index](docs/README.md) |
| Core concepts and the distinction between search methods and optimization problems | [Overview](docs/OVERVIEW.md) |
| General KNIME installation, including Python, Pixi, Conda, pip, and R | [KNIME installation](docs/KNIME_INSTALLATION.md) |
| Installing and verifying IdeaFlow | [IdeaFlow installation](docs/INSTALLATION.md) |
| Complete catalogue of the 27 IdeaFlow nodes | [Node reference](docs/NODES.md) |
| Built-in optimization problems and benchmark references | [Optimization problems](docs/OPTIMIZATION_PROBLEMS.md) |
| Step-by-step construction of a workflow for the Ackley problem | [Workflow tutorial](docs/WORKFLOW_TUTORIAL.md) |
| Reusing workflows through KNIME sub-workflows | [Sub-workflows](docs/SUBWORKFLOWS.md) |
| Java architecture, extension points, and contribution workflow | [Developer guide](docs/DEVELOPMENT.md) |
| Workflow organization and reproducibility | [Best practices](docs/BEST_PRACTICES.md) |
| Common errors and fixes | [Troubleshooting](docs/TROUBLESHOOTING.md) |
| Frequently asked questions | [FAQ](docs/FAQ.md) |

## Implemented algorithms

The following named algorithms are available as IdeaFlow reference recipes. The links point to their foundational or defining research publications.

- **Genetic Algorithm (GA)** — J. H. Holland, [“Genetic Algorithms and the Optimal Allocation of Trials”](https://doi.org/10.1137/0202009), *SIAM Journal on Computing*, 1973.
- **NSGA-II** — K. Deb, A. Pratap, S. Agarwal, and T. Meyarivan, [“A Fast and Elitist Multiobjective Genetic Algorithm: NSGA-II”](https://doi.org/10.1109/4235.996017), *IEEE Transactions on Evolutionary Computation*, 2002.
- **NSGA-III** — K. Deb and H. Jain, [“An Evolutionary Many-Objective Optimization Algorithm Using Reference-Point-Based Nondominated Sorting Approach, Part I”](https://doi.org/10.1109/TEVC.2013.2281535), *IEEE Transactions on Evolutionary Computation*, 2014.
- **Differential Evolution (DE)** — R. Storn and K. Price, [“Differential Evolution — A Simple and Efficient Heuristic for Global Optimization over Continuous Spaces”](https://doi.org/10.1023/A:1008202821328), *Journal of Global Optimization*, 1997.
- **GDE3** — S. Kukkonen and J. Lampinen, [“GDE3: The Third Evolution Step of Generalized Differential Evolution”](https://doi.org/10.1109/CEC.2005.1554717), *IEEE Congress on Evolutionary Computation*, 2005.
- **jDE** — J. Brest, S. Greiner, B. Bošković, M. Mernik, and V. Žumer, [“Self-Adapting Control Parameters in Differential Evolution”](https://doi.org/10.1109/TEVC.2006.872133), *IEEE Transactions on Evolutionary Computation*, 2006.
- **SHADE** — R. Tanabe and A. S. Fukunaga, [“Success-History Based Parameter Adaptation for Differential Evolution”](https://doi.org/10.1109/CEC.2013.6557555), *IEEE Congress on Evolutionary Computation*, 2013.
- **L-SHADE** — R. Tanabe and A. S. Fukunaga, [“Improving the Search Performance of SHADE Using Linear Population Size Reduction”](https://doi.org/10.1109/CEC.2014.6900380), *IEEE Congress on Evolutionary Computation*, 2014.

## Built-in optimization problems

IdeaFlow includes the `ACKLEY`, `SPHERE`, `ROSENBROCK`, `RASTRIGIN`, `GRIEWANK`, `ONEMAX`, `ZDT1`, `ZDT2`, `ZDT3`, and `DTLZ2` benchmark problems.

Their definitions, domains, source publications, and links are documented separately in [Optimization Problems and Benchmarks](docs/OPTIMIZATION_PROBLEMS.md). This separation avoids confusing an optimization **algorithm** with the **problem** it is used to solve.

## Main capabilities

- configurable problem definitions with continuous, integer, binary, and binary-encoded variables;
- custom objective and constraint evaluation through standard [KNIME nodes](https://hub.knime.com/);
- explicit exact-evaluation accounting through [`Finalize Evaluation`](docs/NODES.md#finalize-evaluation);
- single-objective, multi-objective, and many-objective data flows;
- monitoring, archives, migration, quality indicators, and [IOHprofiler](https://iohprofiler.github.io/) export;
- deterministic experiment plans and run-level seeds;
- extensible Java strategies and declarative recipes.

## Current status

- the importable example workflow described in the documentation still has to be exported from [KNIME](https://www.knime.com/knime-analytics-platform) and committed under `examples/workflows/`;
- official distribution through a KNIME update site is separate from manual development installation;
- no integrated machine-learning model is bundled with IdeaFlow; [`Surrogate Selection`](docs/NODES.md#surrogate-selection) consumes predictions produced by KNIME or an external integration.

## Acknowledgements

The authors gratefully acknowledge the support of DS4H (Digital Systems for Humans), whose funding contributed to the research, development, and documentation of the IdeaFlow project.
