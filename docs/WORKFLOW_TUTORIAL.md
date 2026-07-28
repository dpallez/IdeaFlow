# Optimization Workflow Tutorial: Ackley Problem

[Main README](../README.md) · [Documentation index](README.md) · [Overview](OVERVIEW.md) · [KNIME installation](KNIME_INSTALLATION.md) · [IdeaFlow installation](INSTALLATION.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Development](DEVELOPMENT.md) · [Troubleshooting](TROUBLESHOOTING.md)

This tutorial builds a population-based IdeaFlow workflow for the [Ackley optimization problem](https://www.sfu.ca/~ssurjano/ackley). Ackley is the **problem being solved**; it is not an algorithm. Named algorithms and their papers are listed only in [Implemented algorithms](../README.md#implemented-algorithms).

The tutorial uses configurable IdeaFlow operators to demonstrate the complete data flow: problem definition, population creation, evaluation, variation, survivor selection, monitoring, and stopping.

The future importable workflow should be committed at:

```text
examples/workflows/ackley-optimization.knwf
```

<!-- When the workflow is added to the repository, insert the following relative link here: -->
<!-- [Open the Ackley optimization workflow](../examples/workflows/ackley-optimization.knwf) -->

<!-- Planned image: complete screenshot of the final workflow, with annotations separating setup, initial evaluation, loop, and results. -->
<!-- ![Ackley optimization workflow](images/ackley-optimization-workflow.png) -->

## Reference configuration

| Parameter | Value |
| --- | --- |
| Problem | [`ACKLEY`](OPTIMIZATION_PROBLEMS.md#ackley-problem-used-in-the-tutorial) |
| Dimension | `10` |
| Bounds | `[-32.768, 32.768]` |
| Objective | Minimize `fitness` |
| Population size | `50` |
| Maximum exact evaluations | `10,000` |
| Master seed | `42` |
| Parent selection | Tournament, size `2` |
| Crossover | `SBX`, probability `0.9`, index `20` |
| Mutation | Polynomial, automatic rate `1 / 10`, index `20` |
| Survivor update | Single-objective elitist update |

## Workflow structure

```text
Experiment Setup ────────────────┐
                                 ├-> Create Initial Population
Define Optimization Problem ─────┘
       |
       v
Evaluate Benchmark [ACKLEY]
       |
       v
Finalize Evaluation
       |
       v
Evolution Loop Start
       |
       +-> Select Parents -> Crossover -> Mutation
       |                            |
       |                            v
       |                  Evaluate Benchmark [ACKLEY]
       |                            |
       |                            v
       |                  Finalize Evaluation
       |                            |
       +----------------------------+-> Select Survivors (Elitism)
                                            |
                                            v
                                   Optimization Monitor
                                            |
                                            v
                                   Evolution Loop End
```

## 1. Create the experiment plan

Add [`Experiment Setup`](NODES.md#experiment-setup).

| Field | Value |
| --- | --- |
| Experiment ID | `ackley-tutorial` |
| Problem ID | `ackley-10d` |
| Algorithm or recipe | `custom` |
| Replicates | `1` |
| Master seed | `42` |
| Maximum evaluations | `10000` |

The `Algorithm or recipe` field is metadata. The value `custom` avoids presenting the problem name as an algorithm name.

<!-- Planned image: Experiment Setup configuration dialog. -->
<!-- ![Experiment Setup configuration](images/ackley-experiment-setup.png) -->

## 2. Define the optimization problem

Add [`Define Optimization Problem`](NODES.md#define-optimization-problem).

Create one continuous group:

| Field | Value |
| --- | --- |
| Name | `x` |
| Number of variables | `10` |
| First index | `0` |
| Kind of value | `Continuous number` |
| Smallest value | `-32.768` |
| Largest value | `32.768` |

Create one objective:

| Field | Value |
| --- | --- |
| Result column | `fitness` |
| Optimization direction | `Minimize` |
| Hypervolume reference | Disabled |

No constraint is required.

<!-- Planned image: Define Optimization Problem dialog showing the x group and fitness objective. -->
<!-- ![Ackley problem definition](images/ackley-problem-definition.png) -->

## 3. Create and evaluate the initial population

Connect [`Experiment Setup`](NODES.md#experiment-setup) and [`Define Optimization Problem`](NODES.md#define-optimization-problem) to [`Create Initial Population`](NODES.md#create-initial-population).

Configure:

| Field | Value |
| --- | --- |
| Population size | `50` |
| Population ID | `population-0` |

Connect the population to [`Evaluate Benchmark`](NODES.md#evaluate-benchmark):

| Field | Value |
| --- | --- |
| Benchmark | `ACKLEY` |
| Variable columns | `x0` through `x9` |
| Output objective | `fitness` |

Then connect [`Finalize Evaluation`](NODES.md#finalize-evaluation) and select `fitness` as the objective result. This node is the exact-evaluation accounting boundary.

## 4. Open the evolutionary loop

Connect the evaluated population to [`Evolution Loop Start`](NODES.md#evolution-loop-start).

Place [`Evolution Loop End`](NODES.md#evolution-loop-end) at the end of the loop and configure:

| Field | Value |
| --- | --- |
| Maximum generations | `1000` |
| Maximum evaluations | `10000` |
| Stopping objective | `fitness` |
| Direction | `Minimize` |
| Target stopping | Disabled |

The evaluation budget is the main stopping condition.

## 5. Select parents and create candidates

Add [`Select Parents`](NODES.md#select-parents):

| Field | Value |
| --- | --- |
| Objectives | `fitness` |
| Direction | `Minimize` |
| Strategy | `Tournament` |
| Tournament size | `2` |
| Parents per population | `50` |
| Duplicate selections | Enabled |
| Constraint violation column | `__if_constraint_violation` |

Add [`Crossover`](NODES.md#crossover):

| Field | Value |
| --- | --- |
| Variables | `x0` through `x9` |
| Evaluation results to clear | `fitness` |
| Strategy | `SBX` |
| Probability | `0.9` |
| Distribution index | `20` |
| Lower bounds | ten occurrences of `-32.768` |
| Upper bounds | ten occurrences of `32.768` |

Add [`Mutation`](NODES.md#mutation):

| Field | Value |
| --- | --- |
| Variables | `x0` through `x9` |
| Evaluation results to clear | `fitness` |
| Strategy | `Polynomial` |
| Automatic mutation rate | Enabled |
| Distribution index | `20` |
| Lower bounds | ten occurrences of `-32.768` |
| Upper bounds | ten occurrences of `32.768` |

<!-- Planned image: Crossover and Mutation dialogs displayed side by side. -->
<!-- ![Variation operator configuration](images/ackley-variation-settings.png) -->

## 6. Evaluate the new candidates

Add a second [`Evaluate Benchmark`](NODES.md#evaluate-benchmark) configured for `ACKLEY`, followed by a second [`Finalize Evaluation`](NODES.md#finalize-evaluation).

Each loop iteration produces and counts a new batch of exact evaluations.

## 7. Select survivors and monitor progress

Connect:

```text
Evolution Loop Start population -> Select Survivors input 1
Finalized candidate population  -> Select Survivors input 2
```

Configure [`Select Survivors (Elitism)`](NODES.md#select-survivors-elitism):

| Field | Value |
| --- | --- |
| Update mode | `SINGLE_OBJECTIVE` |
| Objective | `fitness` |
| Direction | `Minimize` |
| Constraint violation column | `__if_constraint_violation` |

Add [`Optimization Monitor`](NODES.md#optimization-monitor) after the survivor node, then connect its `Population` output to [`Evolution Loop End`](NODES.md#evolution-loop-end).

## 8. Execute and verify

After execution, inspect:

- `Final population` from [`Evolution Loop End`](NODES.md#evolution-loop-end);
- `Run summary`;
- `Progress summary` and `Detailed events` from [`Optimization Monitor`](NODES.md#optimization-monitor);
- optional files written by [`Export Results`](NODES.md#export-results).

Expected invariants:

- the initial population contains `50` candidates;
- every decision variable remains inside the [Ackley domain](OPTIMIZATION_PROBLEMS.md#ackley-problem-used-in-the-tutorial);
- exact evaluations are counted only by [`Finalize Evaluation`](NODES.md#finalize-evaluation);
- the survivor population remains at size `50`;
- the loop stops at the configured budget or another explicitly enabled limit;
- the final best objective is no worse than the initial best objective under the configured elitist update.

<!-- Planned image: Run summary table and several rows from Final population. -->
<!-- ![Ackley workflow results](images/ackley-results.png) -->

## 9. Export the workflow

Export the validated workflow from [KNIME Analytics Platform](https://www.knime.com/knime-analytics-platform) and commit it as:

```text
examples/workflows/ackley-optimization.knwf
```

Use relative paths and avoid machine-specific resources. See [Best Practices](BEST_PRACTICES.md) and [Sub-workflows](SUBWORKFLOWS.md) before publishing the file.

---

## Related documentation

- [Ackley and the other built-in optimization problems](OPTIMIZATION_PROBLEMS.md)
- [Complete node reference](NODES.md)
- [Sub-workflows](SUBWORKFLOWS.md)
- [Best practices](BEST_PRACTICES.md)
- [Troubleshooting](TROUBLESHOOTING.md)
