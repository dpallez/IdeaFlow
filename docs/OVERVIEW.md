# Concepts and Workflow Architecture

[Main README](../README.md) · [Documentation index](README.md) · [Overview](OVERVIEW.md) · [KNIME installation](KNIME_INSTALLATION.md) · [IdeaFlow installation](INSTALLATION.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Development](DEVELOPMENT.md) · [Troubleshooting](TROUBLESHOOTING.md)


This section introduces the main concepts required to understand how IdeaFlow works in KNIME.

Before building a complete workflow, it is important to understand what the extension is, why KNIME is used as its foundation, and how populations flow between nodes.

<!-- Planned image: screenshot of KNIME with the IdeaFlow category visible in the Node Repository. -->

<!-- Example: ![IdeaFlow category in KNIME](images/ideaflow_node_repository.png) -->

## What is IdeaFlow?

IdeaFlow is an extension for [KNIME Analytics Platform](https://www.knime.com/knime-analytics-platform) developed in [Java](https://openjdk.org/projects/jdk/21/).

It adds a set of nodes to KNIME for building population-based optimization workflows, with a current focus on evolutionary search.

In an IdeaFlow workflow, nodes do not only manipulate standard tables. They also handle data specific to metaheuristics, such as populations, individuals, evaluations, and search parameters.

The extension therefore makes it possible to use KNIME as a graphical design environment for optimization workflows.

## Why use KNIME for metaheuristics?

[KNIME Analytics Platform](https://www.knime.com/knime-analytics-platform) is a visual workflow platform used to build data-processing pipelines from nodes.

This approach is particularly suitable for population-based optimization because the search process is composed of clearly identifiable stages:

* creating an initial population;
* evaluating individuals;
* selecting parents;
* creating new individuals;
* applying mutation or another variation operator;
* replacing the population;
* repeating the process until a stopping condition is reached.

These stages can naturally be represented as a workflow.

Using KNIME also makes it possible to benefit from features documented in the [KNIME user guide](https://docs.knime.com/ap/latest/analytics_platform_user_guide/), such as file reading, table display, visualization, loops, components, and integration with other data-analysis tools.

## Node principle

In KNIME, a node is a component that performs a specific operation.

A node generally has:

* one or more inputs;
* one or more outputs;
* a configuration dialog;
* an execution state;
* optionally, a view for displaying results.

In IdeaFlow, nodes correspond to the main operations of the search process.

For example, a selection node applies a selection strategy to a population. A mutation node applies a random modification to individuals. An evaluation node computes the quality of candidate solutions for a given problem.

<!-- Planned image: close-up of an IdeaFlow node with its input and output ports. -->

<!-- Example: ![Example of an IdeaFlow node](images/ideaflow_node_example.png) -->

The benefit of this approach is that each operation remains independent and reusable. Users can therefore modify one part of the workflow without rewriting the rest.

## Populations as data flows

A population-based metaheuristic manipulates a set of candidate solutions.

With IdeaFlow, this population is treated as data flowing through the workflow. It passes from node to node and is progressively modified by the different operators.

For example:

1. a first node creates the initial population;
2. an evaluation node assigns a fitness value to the individuals;
3. a selection node chooses some individuals as parents;
4. a crossover node creates new individuals;
5. a mutation node modifies these new individuals;
6. a replacement node builds the next population.

This approach makes it possible to visualize how the population evolves throughout the workflow.

<!-- Planned image: diagram showing a population flowing through several nodes. -->

<!-- Example: ![Population as a data flow](images/population_flow.png) -->

## Overall organization of an optimization workflow in KNIME

A population-based optimization workflow in KNIME can be organized into three main parts.

The first part is initialization. It creates the data required to start the workflow, such as the initial population and the optimization problem.

The second part is the main loop. The operators are applied repeatedly to progressively update the population.

The third part is result retrieval and analysis. It is used to inspect the best solution found, the final population, or the resulting fitness values.

A typical structure can therefore be represented as follows:

```text
Initialization -> Initial evaluation -> Generational loop -> Results
```

A generational loop may contain:

```text
Selection -> Crossover -> Mutation -> Evaluation -> Replacement
```

<!-- Planned image: complete workflow screenshot with the three areas visually separated. -->

<!-- Example: ![Overall workflow organization](images/workflow_global_organisation.png) -->


## Search methods and optimization problems are different concepts

A search method determines how candidate solutions are proposed, modified, and selected. An optimization problem defines the decision variables, objectives, constraints, and domain.

- Named methods and their research papers are listed once in [Implemented algorithms](../README.md#implemented-algorithms).
- Built-in problems are documented in [Optimization Problems and Benchmarks](OPTIMIZATION_PROBLEMS.md).
- The practical example is documented as an [Ackley optimization workflow](WORKFLOW_TUTORIAL.md), with the optimization problem clearly separated from the selected search operators.

## Difference between a simple workflow and a sub-workflow

A simple workflow contains all its nodes directly on the main canvas.

This approach is convenient for small examples because the user can see every stage of the algorithm in one place. However, as the workflow becomes more complex, it may become difficult to read.

A [sub-workflow](SUBWORKFLOWS.md) encapsulates part of the workflow in a separate block. The main workflow remains simpler, while the details are placed in the sub-workflow.

In IdeaFlow, a sub-workflow may, for example, contain all the stages of one generation:

```text
Selection -> Crossover -> Mutation -> Evaluation -> Replacement
```

The main workflow can then call this sub-workflow at each iteration.

<!-- Planned image: two screenshots displayed side by side or sequentially: main workflow and sub-workflow contents. -->

<!-- Example: ![Main workflow](images/main_workflow.png) -->

<!-- Example: ![Generation sub-workflow](images/generation_subworkflow.png) -->

Using sub-workflows makes algorithms easier to read, more modular, and easier to reuse.

---
## Related documentation

- [Implemented algorithms and research papers](../README.md#implemented-algorithms)
- [Optimization problems and benchmarks](OPTIMIZATION_PROBLEMS.md)
- [Node reference](NODES.md)
- [Workflow tutorial](WORKFLOW_TUTORIAL.md)
- [KNIME Analytics Platform user guide](https://docs.knime.com/ap/latest/analytics_platform_user_guide/)
