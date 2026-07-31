# Reusing and Encapsulating Workflows

[Main README](../README.md) · [Documentation index](README.md) · [Overview](OVERVIEW.md) · [KNIME installation](KNIME_INSTALLATION.md) · [IdeaFlow installation](INSTALLATION.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Development](DEVELOPMENT.md) · [Troubleshooting](TROUBLESHOOTING.md)


Sub-workflows are a native [KNIME](https://www.knime.com/knime-analytics-platform) feature. They can be used to encapsulate a complete workflow or part of an experimental protocol and then call it from another workflow. IdeaFlow relies on this mechanism to separate search-process definition, execution, and result analysis.

## Sub-workflow principle

A sub-workflow is a KNIME workflow saved independently and exposing input and output ports. The calling workflow passes one or more tables to it, triggers its execution, and then retrieves the produced tables.

In IdeaFlow, a sub-workflow may contain:

- a complete search workflow;
- a specific workflow variant;
- a custom evaluation function;
- a post-processing pipeline;
- a monitoring or export protocol.

## Use cases

Encapsulation is particularly useful for:

- keeping the main workflow readable;
- reusing exactly the same configuration across several experiments;
- comparing several operator configurations from the same experiment;
- isolating a costly evaluation or one that depends on external resources;
- sharing a search workflow without immediately exposing all its details on the main canvas.

It also makes it possible to distinguish two levels: the search workflow, which transforms a population, and the experimental workflow, which organizes replications, comparisons, statistics, and exports.

## Defining inputs and outputs

Ports are defined using KNIME nodes designed for callable workflows, notably:

- [Container Input (Table)](https://hub.knime.com/search?q=Container%20Input%20Table)
- [Container Output (Table)](https://hub.knime.com/search?q=Container%20Output%20Table)

Depending on its role, an IdeaFlow sub-workflow may receive:

- a problem setup or run-parameter table;
- a problem definition;
- an already initialized or evaluated population;
- a parameter table;
- data required for evaluation.

It may return:

- the final population;
- the stopping summary;
- an evaluation history;
- population statistics;
- a Pareto archive;
- a table ready for visualization or export.

Table schemas must remain compatible with the nodes placed before and after the call. Internal columns prefixed with `__if_` must not be removed or renamed while processing an IdeaFlow population.

## Calling a sub-workflow

The main workflow can call a saved workflow using [Call Workflow (Table Based)](https://hub.knime.com/search?q=Call%20Workflow%20Table%20Based).

This node must be configured to point to the target workflow and map its ports to the tables of the main workflow. The called workflow must remain accessible from the environment in which the experiment is executed.

<!-- Planned image: main workflow containing Call Workflow, followed by a screenshot of the called workflow. -->
<!-- ![Calling an IdeaFlow sub-workflow](images/ideaflow-call-workflow.png) -->

## Example: encapsulating the Ackley optimization workflow

The workflow built in the [Ackley problem tutorial](WORKFLOW_TUTORIAL.md) can be encapsulated as a reusable workflow. One possible organization is to expose:

**Inputs:**

- the table produced by **Problem Setup**;
- input data required by a custom evaluator, when applicable.

**Outputs:**

- `Final population`;
- `Run summary`;
- events or statistics collected during execution.

The main workflow can then provide several experiment-plan rows, call the Ackley optimization workflow, and aggregate the results of all replications.

<!-- Planned image: screenshot of the Ackley optimization sub-workflow after it is created. -->
<!-- ![Ackley optimization sub-workflow](images/ackley-optimization-subworkflow.png) -->

## Advantages and limitations

Sub-workflows improve modularity, reusability, and readability. They also simplify comparison protocols because the same main workflow can call several independently configured workflows.

However, they introduce several constraints:

- the called workflow and its resources must be available;
- port and column schemas must be stable;
- the recipient must use a compatible IdeaFlow version;
- excessive encapsulation can hide important stages of the search process;
- local paths, external files, and environment variables should be avoided in workflows intended for sharing.

---
## Related documentation

- [Workflow tutorial](WORKFLOW_TUTORIAL.md)
- [KNIME Community Hub](https://hub.knime.com/)
- [Node reference](NODES.md)
- [Best practices](BEST_PRACTICES.md)
