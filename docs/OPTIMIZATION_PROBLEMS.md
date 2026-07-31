# Optimization Problems and Benchmarks

[Documentation index](README.md) · [Node reference](NODES.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md)

An optimization problem defines the decision variables, objectives, constraints, and search domain. An algorithm defines how candidates are generated and selected. Problem Setup describes the former; Selection, Crossover, Mutation, Elitism, and the loop nodes compose the latter.

## Built-in benchmarks

| Problem | Type | Objectives | Reference |
| --- | --- | ---: | --- |
| `ACKLEY` | Continuous, multimodal | 1 | [Ackley function — SFU Virtual Library](https://www.sfu.ca/~ssurjano/ackley) |
| `SPHERE` | Continuous, convex and unimodal | 1 | [Sphere function — SFU Virtual Library](https://www.sfu.ca/~ssurjano/spheref.html) |
| `ROSENBROCK` | Continuous valley-shaped | 1 | H. H. Rosenbrock, [“An Automatic Method for Finding the Greatest or Least Value of a Function”](https://doi.org/10.1093/comjnl/3.3.175), 1960 |
| `RASTRIGIN` | Continuous, highly multimodal | 1 | [Rastrigin function — SFU Virtual Library](https://www.sfu.ca/~ssurjano/rastr.html) |
| `GRIEWANK` | Continuous, multimodal | 1 | [Griewank function — SFU Virtual Library](https://www.sfu.ca/~ssurjano/griewank.html) |
| `ONEMAX` | Binary pseudo-Boolean | 1 | [OneMax in the IOHprofiler PBO suite](https://iohprofiler.github.io/IOHproblem/PBO) |
| `ZDT1` | Continuous multi-objective | 2 | E. Zitzler, K. Deb, and L. Thiele, [“Comparison of Multiobjective Evolutionary Algorithms: Empirical Results”](https://doi.org/10.1162/106365600568202), 2000 |
| `ZDT2` | Continuous multi-objective | 2 | [ZDT reference](https://doi.org/10.1162/106365600568202) |
| `ZDT3` | Continuous multi-objective | 2 | [ZDT reference](https://doi.org/10.1162/106365600568202) |
| `DTLZ2` | Continuous scalable multi-/many-objective | Configurable | K. Deb et al., [“Scalable Multi-Objective Optimization Test Problems”](https://doi.org/10.1109/CEC.2002.1007032), 2002 |

Evaluation calculates the benchmark objective automatically. Any constraints declared in Problem Setup may still be calculated with formulas.

## Ackley configuration used in the tutorial

| Property | Value |
| --- | --- |
| Dimension | `10` |
| Variables | `x0` through `x9` |
| Domain | `[-32.768, 32.768]` for every variable |
| Objective | minimize `fitness` |
| Known global optimum | `0` at the zero vector |

See the [workflow tutorial](WORKFLOW_TUTORIAL.md) and the importable real-valued Ackley example.

## Custom problems

Problem Setup can describe a custom problem without adding Java code. Evaluation supports two custom paths:

- write objective and constraint formulas in the Evaluation dialog;
- calculate the declared result columns with other KNIME nodes and choose upstream-column evaluation.

Upstream evaluators may use Python, R, simulations, databases, machine-learning models, or external services. They must preserve IdeaFlow columns and produce finite numeric values for every declared objective and constraint. Evaluation then validates the results, calculates total constraint violation, marks the candidates as evaluated, and updates NFE.

## Binary-encoded variables

Problem Setup can represent a bounded numerical value as a configurable number of natural-binary genes. Initial Population creates the genes and Evaluation decodes them automatically before applying a benchmark or formula.
