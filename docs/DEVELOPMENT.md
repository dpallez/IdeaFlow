# Developer Guide

[Main README](../README.md) · [Documentation index](README.md) · [Overview](OVERVIEW.md) · [KNIME installation](KNIME_INSTALLATION.md) · [IdeaFlow installation](INSTALLATION.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Development](DEVELOPMENT.md) · [Troubleshooting](TROUBLESHOOTING.md)


## Developing and Extending IdeaFlow

This section presents the architecture required to add new strategies or nodes to the [IdeaFlow Java project](https://github.com/dpallez/IdeaFlow). It is intended for contributors with a Java and KNIME development environment.

### Source-code organization

The current version is organized around the following packages:

```text
org.ideaflow.v2.api
org.ideaflow.v2.core
org.ideaflow.v2.io
org.ideaflow.v2.knime
org.ideaflow.v2.nodes
org.ideaflow.v2.recipes
org.ideaflow.v2.spi
```

Their general roles are:

| Package | Role |
| --- | --- |
| `api` | Public objects describing candidates, problems, objectives, constraints, events, and run state |
| `core` | Algorithms and utilities independent of the KNIME interface |
| `io` | Event and result export, notably in IOHprofiler format |
| `knime` | Shared integration functions for KNIME tables and settings |
| `nodes` | Factories, models, dialogs, and descriptions for nodes visible in KNIME |
| `recipes` | Declarative recipes describing the strategies required by supported algorithms |
| `spi` | Extension interfaces for replaceable strategies |

Implementations should place search and optimization logic in `core` or behind an interface from `spi` whenever possible, while classes in `nodes` should be limited to configuration, table conversion, and invocation of this logic.

<!-- Planned image: package tree in the IDE. -->
<!-- ![IdeaFlow source-code organization](images/ideaflow-source-tree.png) -->

### Adding a strategy or adding a node

Two forms of extension must be distinguished.

**Adding a strategy to an existing node** is appropriate when the operation already has the same inputs, outputs, and responsibilities. For example, a new bound-repair, ranking, or migration strategy can be integrated behind an SPI interface without creating a new node on the canvas.

**Adding a new KNIME node** is necessary when the operation introduces a new role in the workflow, a new port schema, or configuration that does not match any existing node.

This distinction prevents node proliferation when only the internal method changes.

### General structure of a KNIME node

A [KNIME Java node](https://developer.knime.com/) generally consists of:

- a `NodeFactory`, responsible for creating the node components;
- a `NodeModel`, which defines ports, validates tables, and executes the operation;
- a configuration dialog;
- an XML file describing the node and its help content;
- a declaration in [`plugin.xml`](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/runtime_ext_pt.htm) to register the node in the Node Repository.

The `NodeModel` must notably:

1. declare its inputs and outputs precisely;
2. validate the required columns;
3. reject inconsistent configurations with an explicit message;
4. preserve IdeaFlow columns not modified by the operation;
5. produce a table whose schema remains stable between configuration and execution.

### Creating a new operator strategy

#### Defining the contract

Before implementation, specify:

- the role of the strategy in the search process;
- compatible representations;
- the accepted number of objectives;
- required columns;
- produced or modified properties;
- behavior regarding constraints and seeds.

#### Reusing an SPI interface

The `org.ideaflow.v2.spi` package contains contracts intended for replaceable strategies, including selection, variation, population update, ranking, dominance, archives, stopping criteria, quality indicators, bound repair, and migration topologies.

When an existing interface matches the new operator, the implementation should respect this contract instead of introducing a parallel API.

#### Declaring the strategy

Dynamically discovered strategies are registered through the Java [`ServiceLoader`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html) mechanism. The fully qualified name of the implementation class must be added to the corresponding file under:

```text
META-INF/services/
```

A strategy must expose a stable identifier and a description of its capabilities so that it can be selected and validated by the components that use it.

#### Exposing parameters

If the existing node can host the strategy, its dialog must allow users to select the strategy and enter only the relevant parameters. Non-applicable parameters should be disabled or explicitly ignored.

Every new configuration key must be:

- named consistently and remain stable;
- saved and restored with the workflow;
- validated before execution;
- documented in the node XML file.

#### Preserving reproducibility

A strategy using randomness must derive its random draws from the seeds associated with the run and candidates. It must not create an uncontrolled generator that would make otherwise identical executions non-reproducible.

#### Testing the strategy

Tests must cover at least:

- the nominal case;
- small populations;
- boundary parameter values;
- bounds and constraints;
- reproducibility with a fixed seed;
- invalid data;
- integration in a complete workflow.

### Creating a new visible node

When a new node is required:

1. create its package under `org.ideaflow.v2.nodes`;
2. implement its factory, model, and dialog;
3. add its XML description;
4. define its icon and category;
5. register it in `plugin.xml`;
6. add it to the feature build;
7. verify that it appears in the Node Repository;
8. create a test workflow covering its ports and parameters.

The visible name should describe an operation, remain consistent with existing nodes, and avoid abbreviations that are difficult to understand.

### Validation before contributing

Before proposing a change:

- build the plugin and feature;
- run the core tests;
- run export tests when relevant;
- open KNIME with the updated plugin;
- create or update a test workflow;
- verify that the workflow can be saved and reopened;
- update the documentation and changelog.

The authoritative continuous-integration and release build runs in Jenkins. See the
[testing guide](TESTING.md) for the local suite and the [Jenkins pipeline guide](JENKINS.md) for agent requirements, build stages, artifacts, and release
tag rules.

---
## Creating a New Search Recipe

In IdeaFlow, a named search configuration is primarily represented by a composition of nodes and strategies. Creating a new recipe therefore does not systematically require a new node.

### Defining the search cycle

First describe the complete sequence:

```text
initialization
-> evaluation
-> candidate proposal
-> candidate evaluation
-> population update
-> optional adaptation or archiving
-> measurements
-> stopping
```

For each stage, specify whether it operates on parents, offspring, an archive, individual parameters, or the global run state.

### Identifying the required strategies

Compare the needs of the algorithm with the nodes and strategies already available:

- representation and initialization;
- parent selection;
- crossover or differential generation;
- mutation;
- constraint handling;
- survivor selection;
- multi-objective ranking;
- archive;
- parameter adaptation;
- population reduction;
- stopping criteria;
- quality indicators.

A new implementation is required only when no existing strategy satisfies the contract of the stage.

### Developing the missing elements

Missing elements must be added according to the distinction presented in Section 9: a new strategy behind an existing SPI, or a new node when the operation introduces a new role in the workflow.

Columns produced by a stage must be documented, especially when they are required by the following stage.

### Adding the declarative recipe

Built-in recipes are described under:

```text
org/ideaflow/v2/recipes/
```

A new recipe must define a stable identifier, the expected strategies, and compatibility constraints useful for its validation. It must also be added to the recipe index when this index is used during loading.

The recipe describes a reference configuration; it does not replace the KNIME workflow that concretely shows the node sequence.

### Building the reference workflow

The reference workflow must:

- use the nodes corresponding to the recipe;
- make essential stages visible;
- define consistent and documented parameters;
- avoid any dependency on a local path;
- use a deterministic seed;
- produce a final population and an execution summary;
- preserve results that can be used for testing.

### Testing on a suitable benchmark

The benchmark must match the problem domain:

- `ONEMAX` for a binary recipe;
- `SPHERE`, `ACKLEY`, `RASTRIGIN`, `ROSENBROCK`, or `GRIEWANK` for a continuous single-objective method;
- `ZDT1`, `ZDT2`, or `ZDT3` for a multi-objective method;
- `DTLZ2` for a many-objective method.

Tests should verify the workflow structure and method invariants without depending on one unique final value when the method is stochastic.

### Documenting the recipe

The documentation must specify the recipe contract and link back to [Implemented algorithms](../README.md#implemented-algorithms) only when the recipe corresponds to a named method:

- recipe family;
- compatible representations;
- number of objectives;
- used nodes and strategies;
- main parameters;
- reference workflow;
- known limitations;
- useful bibliographic references.

---
## Related documentation

- [KNIME developer resources](https://developer.knime.com/)
- [Java ServiceLoader API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html)
- [Eclipse extension registry](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/runtime_ext_pt.htm)
- [Node reference](NODES.md)
- [Best practices](BEST_PRACTICES.md)
