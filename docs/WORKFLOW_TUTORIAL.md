# Optimization Workflow Tutorial

[Documentation index](README.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Examples](../ideaflow-knime/examples/EXAMPLES.md)

This tutorial describes the current IdeaFlow data flow for a real-valued Ackley genetic algorithm. For an importable implementation, use `2 Real-Valued Ackley GA.knwf` from the examples folder.

## Reference configuration

| Setting | Value |
| --- | --- |
| Experiment name | `ackley-ga` |
| Seed | `42` |
| Maximum evaluations | `10000` |
| Variables | ten float variables named `x0` to `x9` |
| Bounds | `-32.768` to `32.768` |
| Objective | minimize `fitness` |
| Population size | `50` |
| Selection | tournament, size `2` |
| Crossover | SBX, probability `0.9`, distribution index `20` |
| Mutation | polynomial, automatic rate, distribution index `20` |
| Survivor update | single-objective elitism |

## Workflow structure

```text
Problem Setup -> Initial Population -> Initial Evaluation -> Optimization Loop Start
                                                               |
                         Selection -> Crossover -> Mutation -> Evaluation
                                                               |
current population -----------------------------------------> Elitism
                                                               |
                            Track Progress -> Optimization Loop End
```

The same Problem Setup output is connected to both Evaluation nodes. It carries the variables, objectives, constraints, and run settings required to validate each population.

## 1. Configure Problem Setup

Add **Problem Setup** and configure one run:

- experiment name `ackley-ga`;
- seed `42`;
- maximum evaluations `10000`.

Add one direct variable group with name `x`, count `10`, type `Float`, and bounds `-32.768` and `32.768`. Add one objective named `fitness`, choose `Minimize`, and leave constraints empty.

Problem Setup produces a readable definition table and attaches the same definition as metadata for downstream IdeaFlow nodes.

## 2. Create and evaluate the initial population

Connect Problem Setup to **Initial Population**. Set the population size to `50` and use a meaningful population ID such as `population-0`.

Connect the population and Problem Setup to **Evaluation**. Choose the built-in `ACKLEY` benchmark. Evaluation calculates the objective, marks candidates as evaluated, advances NFE, and produces evaluation-history events.

## 3. Add the optimization loop

Connect the evaluated population to **Optimization Loop Start**. The optional archive input is unnecessary for this GA workflow.

Place **Optimization Loop End** after the generation body. With no objective target, the loop uses the maximum-evaluations value from Problem Setup as its strict stopping budget.

## 4. Create offspring

Connect Loop Start's current population to **Selection** and select tournament mode. Set tournament size to `2`, request the number of parents needed by the workflow, and allow duplicates if desired.

Connect **Crossover** and choose SBX with probability `0.9` and distribution index `20`. Then connect **Mutation** and choose polynomial mutation with the automatic rate. The automatic rate is one divided by the number of variables.

Connect the candidates and Problem Setup to the second **Evaluation** node, again using the Ackley benchmark.

## 5. Select the next population

Connect Loop Start's current population to the first **Elitism** input and the evaluated offspring to its second input. Choose the single-objective `Elitism` update mode and a fixed population-size policy.

Elitism reads the objective direction and constraint definitions from Problem Setup metadata. Its first output contains the next population; its second output contains candidates that were rejected or replaced.

## 6. Track and stop

Connect the survivors to **Track Progress**. Its first output passes the population through unchanged and goes to Optimization Loop End's `Next population` input. Connect its compact `Progress summary` output to the loop end's optional progress input.

After execution, inspect:

- `Final population` for the surviving candidates;
- `Run summary` for final NFE, best value, and stop reason;
- `Convergence history` for the summaries accumulated during the loop.

Expected invariants include a population of 50 candidates, variables inside the Ackley bounds, a monotonically increasing NFE counter that never exceeds 10000, and a final best objective no worse than the initial best objective under elitism.

## Adapting the workflow

- Replace the built-in benchmark with formulas or upstream result columns for a custom problem.
- Choose DE donor selection, differential mutation, DE crossover, and pairwise DE competition for Differential Evolution.
- Feed Loop Start's archive to Selection and return replaced parents through Loop End for SHADE or L-SHADE.
- Choose NSGA-II, NSGA-III, or GDE3 in Elitism for multi-objective workflows.
- Use the [included examples](../ideaflow-knime/examples/EXAMPLES.md) for complete configurations of these families.
