# Optimization Problems and Benchmarks

[Main README](../README.md) · [Documentation index](README.md) · [Overview](OVERVIEW.md) · [KNIME installation](KNIME_INSTALLATION.md) · [IdeaFlow installation](INSTALLATION.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Development](DEVELOPMENT.md) · [Troubleshooting](TROUBLESHOOTING.md)

An optimization problem defines the decision variables, objectives, constraints, and search domain. A search algorithm defines how candidate solutions are generated and selected. These are separate concepts: the list of named methods is centralized in the [Implemented algorithms](../README.md#implemented-algorithms) section, while this document covers the problems available through [`Evaluate Benchmark`](NODES.md#evaluate-benchmark).

## Built-in problems

| Problem | Type | Objectives | Reference or definition |
| --- | --- | ---: | --- |
| `ACKLEY` | Continuous, multimodal | 1 | [Ackley function — SFU Virtual Library](https://www.sfu.ca/~ssurjano/ackley) |
| `SPHERE` | Continuous, convex and unimodal | 1 | [Sphere function — SFU Virtual Library](https://www.sfu.ca/~ssurjano/spheref.html) |
| `ROSENBROCK` | Continuous valley-shaped problem | 1 | H. H. Rosenbrock, [“An Automatic Method for Finding the Greatest or Least Value of a Function”](https://doi.org/10.1093/comjnl/3.3.175), 1960; [function page](https://www.sfu.ca/~ssurjano/rosen.html) |
| `RASTRIGIN` | Continuous, highly multimodal | 1 | [Rastrigin function — SFU Virtual Library](https://www.sfu.ca/~ssurjano/rastr.html) |
| `GRIEWANK` | Continuous, multimodal | 1 | [Griewank function — SFU Virtual Library](https://www.sfu.ca/~ssurjano/griewank.html) |
| `ONEMAX` | Binary pseudo-Boolean | 1 | [OneMax in the IOHprofiler PBO suite](https://iohprofiler.github.io/IOHproblem/PBO) |
| `ZDT1` | Continuous multi-objective | 2 | E. Zitzler, K. Deb, and L. Thiele, [“Comparison of Multiobjective Evolutionary Algorithms: Empirical Results”](https://doi.org/10.1162/106365600568202), 2000 |
| `ZDT2` | Continuous multi-objective | 2 | [Same ZDT reference paper](https://doi.org/10.1162/106365600568202) |
| `ZDT3` | Continuous multi-objective | 2 | [Same ZDT reference paper](https://doi.org/10.1162/106365600568202) |
| `DTLZ2` | Continuous scalable multi-/many-objective | Configurable | K. Deb, L. Thiele, M. Laumanns, and E. Zitzler, [“Scalable Multi-Objective Optimization Test Problems”](https://doi.org/10.1109/CEC.2002.1007032), 2002 |

## Ackley problem used in the tutorial

The [workflow tutorial](WORKFLOW_TUTORIAL.md) uses the [`ACKLEY`](https://www.sfu.ca/~ssurjano/ackley) benchmark with:

| Property | Value |
| --- | --- |
| Dimension | `10` |
| Decision variables | `x0` through `x9` |
| Domain | `[-32.768, 32.768]` for each variable |
| Objective column | `fitness` |
| Direction | Minimize |
| Known global optimum | `0` at the zero vector |

The tutorial demonstrates how to define and solve this **problem** with an IdeaFlow workflow while keeping the problem definition separate from the selected search operators.

## Using a custom problem

A custom evaluation can be implemented with any compatible [KNIME nodes](https://hub.knime.com/), a [Python Script](https://docs.knime.com/ap/latest/python_installation_guide/), an [R integration](https://docs.knime.com/ap/latest/r_installation_guide/), or an external service.

The resulting table must contain the declared objective and constraint columns before it enters [`Finalize Evaluation`](NODES.md#finalize-evaluation).

<!-- Planned image: diagram separating the optimization problem definition from the search workflow. -->
<!-- ![Optimization problem and search workflow](images/problem-and-search-workflow.png) -->

---

## Related documentation

- [Define Optimization Problem](NODES.md#define-optimization-problem)
- [Evaluate Benchmark](NODES.md#evaluate-benchmark)
- [Workflow tutorial](WORKFLOW_TUTORIAL.md)
- [Best practices: separating the search process from the problem](BEST_PRACTICES.md#separating-the-search-process-from-the-problem)
- [Implemented algorithms and research papers](../README.md#implemented-algorithms)
