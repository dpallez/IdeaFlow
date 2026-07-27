# IdeaFlow
Interactive Design of Evolutionary Algorithms as a workFlow

## 1. Introduction

IdeaFlow is a KNIME extension dedicated to the graphical design of metaheuristics, particularly algorithms that manipulate a population of candidate solutions.

Its general objective is to allow users to build an optimization algorithm as a workflow by assembling nodes corresponding to the different stages of the algorithm: initialization, evaluation, selection, crossover, mutation, replacement, and result retrieval.

<!-- Planned image: overall screenshot of a complete IdeaFlow workflow in KNIME. -->

<!-- Example: ![Overview of an IdeaFlow workflow](images/ideaflow_workflow_overview.png) -->

### 1.1. Tutorial objective

This tutorial presents how to use the IdeaFlow extension in KNIME.

It explains how to install the extension, where to find its nodes in the KNIME interface, and how to understand the overall organization of a metaheuristic workflow.

It also documents the available algorithmic recipes, the nodes provided by the extension, and the mechanisms intended for adding new strategies or workflows.

### 1.2. Intended audience

This tutorial is primarily intended for users who want to design or experiment with metaheuristics in a graphical environment.

It may be useful to several audiences:

* students discovering evolutionary algorithms;
* teachers who want to illustrate how a metaheuristic works visually;
* researchers or practitioners who want to quickly test different combinations of operators;
* developers who want to understand how the extension can be enhanced with new nodes.

No advanced knowledge of KNIME is required to follow the first sections of the tutorial. However, the sections related to extending the tool or developing new operators require knowledge of Java and KNIME extension development.

### 1.3. What the extension provides

IdeaFlow makes it possible to build search and optimization algorithms as KNIME workflows.

The extension provides nodes representing the main stages of a population-based metaheuristic. These nodes can be assembled graphically to create a complete algorithm.

The main idea is to replace a conventional fully coded implementation with a visual representation that is easier to read and modify.

For example, in a genetic algorithm, the user can assemble a workflow containing:

* a population initialization node;
* an evaluation node;
* a selection node;
* a crossover node;
* a mutation node;
* a replacement node;
* one or more result visualization or export nodes.

<!-- Planned image: simple diagram or annotated screenshot showing an Initialization -> Evaluation -> Loop -> Results chain. -->

<!-- Example: ![General structure of a genetic algorithm](images/ga_structure.png) -->

### 1.4. General principle: designing metaheuristics as workflows

In KNIME, a workflow is a chain of connected nodes.

Each node receives input data, performs an operation, and then produces output data. Connections between nodes therefore indicate the path followed by the data.

IdeaFlow applies this principle to metaheuristics: the population of candidate solutions flows between the different nodes of the workflow.

Each node represents one stage of the algorithm. For example, a mutation node receives a population or a set of individuals, applies a random modification to the solutions, and returns the modified population.

This representation makes the algorithm easier to read because its structure is directly visible on the KNIME canvas. Users can quickly identify the different stages, replace an operator, change a parameter, or compare several variants.

### 1.5. Inspiration: metaheuristic libraries and KNIME workflows

Many metaheuristic libraries already exist in different programming languages. They make it possible to develop powerful algorithms, but they often require writing code and understanding the architecture of the selected library in detail.

IdeaFlow draws on this reusable-component approach and adapts it to the graphical KNIME environment.

Instead of directly calling classes or functions in a program, users manipulate configurable nodes through the interface. Each node corresponds to a precise algorithmic operation.

The objective is therefore not only to provide an algorithm library, but also to offer a visual way to build, modify, and experiment with these algorithms.

---

## 2. General overview of the extension

This section introduces the main concepts required to understand how IdeaFlow works in KNIME.

Before building a complete workflow, it is important to understand what the extension is, why KNIME is used as its foundation, and how populations flow between nodes.

<!-- Planned image: screenshot of KNIME with the IdeaFlow category visible in the Node Repository. -->

<!-- Example: ![IdeaFlow category in KNIME](images/ideaflow_node_repository.png) -->

### 2.1. What is IdeaFlow?

IdeaFlow is a KNIME extension developed in Java.

It adds a set of nodes to KNIME for building population-based metaheuristics, with a current focus on evolutionary algorithms.

In an IdeaFlow workflow, nodes do not only manipulate standard tables. They also handle data specific to metaheuristics, such as populations, individuals, evaluations, and algorithm parameters.

The extension therefore makes it possible to use KNIME as a graphical design environment for optimization algorithms.

### 2.2. Why use KNIME for metaheuristics?

KNIME is a visual workflow platform used to build data-processing pipelines from nodes.

This approach is particularly suitable for metaheuristics because these algorithms are usually composed of clearly identifiable stages:

* creating an initial population;
* evaluating individuals;
* selecting parents;
* creating new individuals;
* applying mutation or another variation operator;
* replacing the population;
* repeating the process until a stopping condition is reached.

These stages can naturally be represented as a workflow.

Using KNIME also makes it possible to benefit from features already available in the platform, such as file reading, table display, visualization, loops, components, and integration with other data-analysis tools.

### 2.3. Node principle

In KNIME, a node is a component that performs a specific operation.

A node generally has:

* one or more inputs;
* one or more outputs;
* a configuration dialog;
* an execution state;
* optionally, a view for displaying results.

In IdeaFlow, nodes correspond to the main operations of a metaheuristic algorithm.

For example, a selection node applies a selection strategy to a population. A mutation node applies a random modification to individuals. An evaluation node computes the quality of candidate solutions for a given problem.

<!-- Planned image: close-up of an IdeaFlow node with its input and output ports. -->

<!-- Example: ![Example of an IdeaFlow node](images/ideaflow_node_example.png) -->

The benefit of this approach is that each operation remains independent and reusable. Users can therefore modify one part of the algorithm without rewriting the rest.

### 2.4. Populations as data flows

A population-based metaheuristic manipulates a set of candidate solutions.

With IdeaFlow, this population is treated as data flowing through the workflow. It passes from node to node and is progressively modified by the different operators.

For example:

1. a first node creates the initial population;
2. an evaluation node assigns a fitness value to the individuals;
3. a selection node chooses some individuals as parents;
4. a crossover node creates new individuals;
5. a mutation node modifies these new individuals;
6. a replacement node builds the next population.

This approach makes it possible to visualize how the population evolves throughout the algorithm.

<!-- Planned image: diagram showing a population flowing through several nodes. -->

<!-- Example: ![Population as a data flow](images/population_flow.png) -->

### 2.5. Overall organization of an algorithm in KNIME

A metaheuristic algorithm in KNIME can be organized into three main parts.

The first part is initialization. It creates the data required to start the algorithm, such as the initial population and the optimization problem.

The second part is the main loop. The operators are applied repeatedly in this loop to progressively improve the population.

The third part is result retrieval and analysis. It is used to inspect the best solution found, the final population, or the resulting fitness values.

A typical structure can therefore be represented as follows:

```text
Initialization -> Initial evaluation -> Generational loop -> Results
```

For a genetic algorithm, the generational loop may contain:

```text
Selection -> Crossover -> Mutation -> Evaluation -> Replacement
```

<!-- Planned image: complete workflow screenshot with the three areas visually separated. -->

<!-- Example: ![Overall workflow organization](images/workflow_global_organisation.png) -->

### 2.6. Difference between a simple workflow and a sub-workflow

A simple workflow contains all its nodes directly on the main canvas.

This approach is convenient for small examples because the user can see every stage of the algorithm in one place. However, as the workflow becomes more complex, it may become difficult to read.

A sub-workflow encapsulates part of the workflow in a separate block. The main workflow remains simpler, while the details are placed in the sub-workflow.

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

## 3. Installing the extension

This section explains how to install the IdeaFlow extension in KNIME and verify that its nodes are available in the Node Repository.

> **Distribution status:** until publication on a KNIME update site is finalized, the manual installation described in Section 3.4 is the reference testing method.

The recommended method is to install IdeaFlow directly from KNIME when the extension is available as an add-on or through an update site. Depending on the selected distribution method, the extension may also be obtained from a project website, for example through an installation link, archive, or `.jar` file.

Manual installation remains possible by placing the extension `.jar` file directly in KNIME's `dropins` directory. This method is useful for testing a local or unpublished version, but it is less convenient for updates than installation through KNIME.

<!-- Planned image: screenshot of KNIME showing how to access the extension installation menu. -->

<!-- Example: ![Installing the extension](images/install_extension_menu.png) -->

### 3.1. Requirements

Before installing the extension, a compatible KNIME environment is required.

The required elements generally include:

* a compatible version of KNIME Analytics Platform;
* a functional KNIME workspace;
* optionally, access to the website on which the extension is published;
* optionally, the extension `.jar` file for manual installation.

In a standard installation, end users do not normally need to configure Java themselves because KNIME provides its own runtime environment. However, the Java version remains important for development and compatibility because the extension is developed in Java.

It is recommended to check the KNIME version before installation, especially while the extension is still under development or distributed as a test version.

#### 3.1.1. Required KNIME version

The extension must be installed on a KNIME version compatible with the developed nodes.

```text
Minimum recommended version: 5.11
Tested version: 5.11
```

To check the installed KNIME version, open:

```text
Help -> About KNIME Analytics Platform
```

or, depending on the interface:

```text
File -> About KNIME Analytics Platform
```

<!-- Planned image: screenshot of the "About KNIME Analytics Platform" window showing the installed version. -->

<!-- Example: ![KNIME version](images/knime_version.png) -->

#### 3.1.2. Required Java version

Because IdeaFlow is developed in Java, its compatibility depends on the Java environment used by KNIME.

End users generally do not need to install Java separately if KNIME already provides its own runtime environment. This information mainly indicates the version used to develop and test the extension.

```text
Recommended Java version: JDK 21
Tested version: JDK 21
```

If the extension fails to load, it may be useful to verify that the installed KNIME version is compatible with the Java version targeted by the project.

#### 3.1.3. Required dependencies

```text
No external dependency needs to be installed manually.
```

### 3.2. Installation from KNIME

The recommended method is to install IdeaFlow directly from KNIME as a standard extension or add-on.

Open KNIME, then go to:

```text
Menu -> Install extensions...
```

Search for:

```text
IdeaFlow
```

Select the extension from the list, then follow the installation steps provided by KNIME.

After installation, restart KNIME if prompted.

<!-- Planned image: screenshot of the "Install KNIME Extensions" window with IdeaFlow entered in the search field. -->

<!-- Example: ![Searching for the IdeaFlow extension](images/install_knime_extensions_ideaflow.png) -->

<!-- Planned image: screenshot showing the extension selected before installation. -->

<!-- Example: ![Selecting the IdeaFlow extension](images/select_ideaflow_extension.png) -->

### 3.3. Installation from the project website or an update site

When an update site is provided, it must first be added to KNIME's list of known software sites.

Open KNIME, then go to:

```text
File -> Preferences -> Install/Update -> Available Software Sites
```

Click:

```text
Add...
```

Then enter the address or location of the update site specified on the publication page for the IdeaFlow version being installed.

If the update site is provided as a local directory or archive, select the corresponding location using the appropriate KNIME option.

After adding the update site, install the extension from:

```text
File -> Install KNIME Extensions...
```

Search for:

```text
IdeaFlow
```

Select the extension, confirm the installation, and restart KNIME if required.

<!-- Planned image: screenshot of the project website showing the update site or download link. -->

<!-- Example: ![IdeaFlow project website](images/ideaflow_project_website.png) -->

<!-- Planned image: screenshot of the "Available Software Sites" window with the IdeaFlow update site added. -->

<!-- Example: ![Adding the IdeaFlow update site](images/available_software_sites_ideaflow.png) -->

<!-- Planned image: screenshot of the "Install KNIME Extensions" window with IdeaFlow entered in the search field. -->

<!-- Example: ![Installing IdeaFlow from KNIME](images/install_knime_extensions_ideaflow.png) -->


### 3.4. Manual installation using a `.jar` file

If the extension is not yet available from KNIME or an update site, it can be installed manually from the `.jar` file.

This method is mainly intended for testing a local version, a development version, or a version provided directly by the developers.

To install the extension manually:

1. close KNIME;
2. obtain the IdeaFlow extension `.jar` file;
3. open the KNIME installation directory;
4. place the `.jar` file in the `dropins` directory;
5. restart KNIME;
6. verify that the nodes appear in the Node Repository.

The exact path to the `dropins` directory depends on the operating system and on where KNIME was installed.

Possible path examples:

```text
Windows: C:\...\KNIME\dropins
Linux: /.../knime/dropins
macOS: /.../KNIME.app/.../dropins
```

These paths must be adapted to the local installation.

<!-- Planned image: screenshot of the KNIME installation directory showing the dropins folder. -->

<!-- Example: ![KNIME dropins directory](images/knime_dropins_folder.png) -->

<!-- Planned image: screenshot of the IdeaFlow JAR file placed in the dropins directory. -->

<!-- Example: ![IdeaFlow JAR file in dropins](images/ideaflow_jar_dropins.png) -->

This method does not handle updates as cleanly as an update site. For regular use, installation from KNIME or an update site should therefore be preferred once available.

### 3.5. Verifying the installation

After installing the extension and restarting KNIME, open the Node Repository.

Search for the extension name or the name of an available node.

For example:

```text
IdeaFlow
```

or the exact name of an implemented node, such as:

```text
Crossover
```

```text
Select Parents
```

If the installation is successful, the extension nodes should appear in the Node Repository.

<!-- Planned image: screenshot of the Node Repository with "IdeaFlow" in the search field. -->

<!-- Example: ![Searching for IdeaFlow in the Node Repository](images/search_ideaflow_node_repository.png) -->

<!-- Planned image: screenshot of an empty workflow with a first IdeaFlow node placed on the canvas. -->

<!-- Example: ![First IdeaFlow node in a workflow](images/first_ideaflow_node.png) -->

### 3.6. Node location in KNIME

IdeaFlow nodes are available from the KNIME Node Repository.

```text
Node Repository -> IdeaFlow
```

or, if the extension is published as a community extension:

```text
Node Repository -> Community Nodes -> IdeaFlow
```

Each node can then be added to a workflow by dragging and dropping it onto the canvas.

Once the extension is installed, IdeaFlow nodes appear in the Node Repository under the categories declared by the extension:

- `Setup`;
- `Evolution`;
- `Evaluation`;
- `Results`;
- `Advanced`;
- `Advanced / Utilities`.

<!-- Planned image: screenshot of the full category containing IdeaFlow nodes. -->

<!-- Example: ![Location of IdeaFlow nodes](images/ideaflow_nodes_location.png) -->

### 3.7. Common installation issues

This section lists the most common problems that may occur while installing the extension.

#### 3.7.1. The extension does not appear

If the extension does not appear in KNIME after installation, verify that:

* KNIME was restarted after installation;
* the installed KNIME version is compatible with the extension;
* the update site or installation files are correct;
* the installation was not interrupted;
* the `.jar` file was placed in the `dropins` directory for a manual installation;
* no error message appeared in the console or logs.

It may also be useful to search directly for the name of one of the nodes in the Node Repository.

For a manual `.jar` installation, also verify that an older version of the `.jar` file is not still present in the `dropins` directory, as multiple versions may conflict.

#### 3.7.2. Nodes do not execute

If nodes appear but cannot be executed, verify that:

* all required ports are connected;
* the preceding nodes have been executed;
* the input data has the expected type;
* node parameters are correctly configured;
* the KNIME version is compatible;
* all required dependencies are available.

The error messages displayed on KNIME nodes generally help identify the cause of the problem.

<!-- Planned image: screenshot of a node in an error state with its error message displayed. -->

<!-- Example: ![Node execution error](images/node_error.png) -->

#### 3.7.3. Java version issue

An incompatible Java version may prevent the extension from loading or executing.

If a Java-related error appears, verify:

* the Java version used by KNIME;
* the Java version used to develop the extension;
* compatibility between both versions;
* the runtime environment configuration.

```text
Recommended Java version: JDK 21
```

#### 3.7.4. Dependency issue

When a dependency is missing, some nodes may fail to appear or execute correctly.

In this case, verify that all required libraries are included in the extension or installed in the KNIME environment.

When the extension is distributed as a complete package, dependencies should normally be included in the installation.

#### 3.7.5. Manual installation issue

When installing manually from a `.jar` file, several issues may occur:

* the file was not placed in the correct `dropins` directory;
* KNIME was not restarted after the file was added;
* an older `.jar` version is still present;
* the `.jar` file was not built correctly;
* the extension is incompatible with the installed KNIME version.

Whenever possible, prefer installation from KNIME or an update site for officially distributed versions.

---

## 4. Available algorithms and recipes

IdeaFlow provides a set of generic nodes that can be used to compose metaheuristics as KNIME workflows. The algorithms presented in this section are not implemented as monolithic black boxes: they are described by reference recipes that associate stages, strategies, and compatibility constraints.

The recipes included in the extension currently cover eight algorithms or algorithm families: GA, NSGA-II, NSGA-III, DE, GDE3, jDE, SHADE, and L-SHADE.

<!-- Planned image: overview screenshot of the Node Repository with the IDEAFlow categories expanded. -->
<!-- ![IdeaFlow categories in the Node Repository](images/ideaflow-node-repository.png) -->

### 4.1. Genetic Algorithm — GA

The **Genetic Algorithm** recipe describes a generational genetic algorithm built from the following stages:

```text
initialization
-> initial evaluation
-> parent selection
-> crossover
-> mutation
-> offspring evaluation
-> elitist replacement
-> result monitoring
-> stopping
```

In IdeaFlow, this recipe mainly relies on the following nodes:

```text
Experiment Setup
Define Optimization Problem
Create Initial Population
Evaluate Benchmark or custom KNIME evaluation
Finalize Evaluation
Evolution Loop Start
Select Parents
Crossover
Mutation
Select Survivors (Elitism)
Optimization Monitor
Evolution Loop End
```

The reference recipe uses tournament selection and single-objective elitist replacement. The generic operators allow the workflow to be adapted to several representations:

- continuous variables, using SBX or arithmetic crossover and polynomial or Gaussian mutation;
- integer variables, using uniform or one-point crossover and random-reset mutation;
- binary variables, using uniform or one-point crossover and bit-flip mutation;
- binary-encoded numerical variables, using **Decode Binary Variables** before evaluation.

The tutorial in Sections 6 and 7 builds a real-coded GA for the Ackley function.

### 4.2. NSGA-II

**NSGA-II** is a multi-objective evolutionary algorithm recipe. It aims to preserve a set of solutions representing different trade-offs between several objectives instead of a single best solution.

The IdeaFlow recipe uses:

- fast non-dominated sorting;
- a diversity measure based on crowding distance;
- parent selection that accounts for Pareto rank and diversity;
- environmental replacement configured in `NSGA_II` mode;
- an optional archive of non-dominated solutions and quality indicators.

Reference structure:

```text
Create Initial Population
-> multi-objective evaluation
-> Finalize Evaluation
-> Evolution Loop Start
-> Pareto Ranking & Diversity
-> Select Parents
-> Crossover
-> Mutation
-> multi-objective evaluation
-> Finalize Evaluation
-> Select Survivors (Elitism) [NSGA_II]
-> Update Archive [PARETO]
-> Quality Indicators
-> Evolution Loop End
```

The recipe requires at least two objectives. The `ZDT1`, `ZDT2`, and `ZDT3` benchmarks provided by **Evaluate Benchmark** can be used to quickly build validation workflows.

### 4.3. NSGA-III

**NSGA-III** extends the multi-objective selection principle to problems with at least three objectives. Diversity is no longer maintained solely through crowding distance: it relies on reference directions distributed over the objective simplex.

In IdeaFlow, the recipe uses:

- non-dominated sorting;
- Das-Dennis reference directions;
- replacement configured in `NSGA_III` mode;
- a niching mechanism associated with the reference directions.

Reference structure:

```text
Generate Reference Directions
Create Initial Population
-> many-objective evaluation
-> Finalize Evaluation
-> Evolution Loop Start
-> Pareto Ranking & Diversity
-> Select Parents
-> Crossover
-> Mutation
-> many-objective evaluation
-> Finalize Evaluation
-> Select Survivors (Elitism) [NSGA_III]
-> Update Archive [PARETO]
-> Quality Indicators
-> Evolution Loop End
```

The `DTLZ2` benchmark is available for testing this recipe. The number of reference-direction divisions can be configured in **Generate Reference Directions** and in the NSGA-III mode of **Select Survivors (Elitism)**.

### 4.4. Differential Evolution — DE

**Differential Evolution** is a continuous optimization method in which new candidates are built from differences between several individuals in the population.

The reference DE recipe uses the `DE/rand/1` strategy with binomial crossover and parent-offspring competition.

Reference structure:

```text
Create Initial Population
-> initial evaluation
-> Finalize Evaluation
-> Evolution Loop Start
-> Differential Evolution
-> trial-candidate evaluation
-> Finalize Evaluation
-> Select Survivors (Elitism) [DE_PAIRWISE]
-> Optimization Monitor
-> Evolution Loop End
```

The **Differential Evolution** node provides several differential mutation strategies:

- `RAND_1`;
- `BEST_1`;
- `CURRENT_TO_BEST_1`;
- `CURRENT_TO_PBEST_1`.

It also provides binomial or exponential crossover and several bound-repair policies: reflection, saturation at the nearest bound, or generation of a new valid value.

### 4.5. GDE3

**GDE3** applies Differential Evolution principles to multi-objective optimization. Competition between a parent and its trial candidate accounts for Pareto dominance. When neither solution dominates the other, both may be retained before environmental reduction.

Reference structure:

```text
Create Initial Population
-> multi-objective evaluation
-> Finalize Evaluation
-> Evolution Loop Start
-> Differential Evolution
-> multi-objective evaluation
-> Finalize Evaluation
-> Select Survivors (Elitism) [GDE3]
-> Pareto Ranking & Diversity
-> Update Archive [PARETO]
-> Quality Indicators
-> Evolution Loop End
```

This recipe requires real-valued variables and at least two objectives. The ZDT benchmarks are suitable test cases.

### 4.6. jDE

**jDE** is a self-adaptive Differential Evolution variant. The `F` and `CR` parameters are associated with individuals and may be resampled during execution.

The IdeaFlow recipe adds **Adapt DE Parameters** before candidate generation:

```text
Create Initial Population
-> initial evaluation
-> Finalize Evaluation
-> Evolution Loop Start
-> Adapt DE Parameters [JDE]
-> Differential Evolution
-> trial-candidate evaluation
-> Finalize Evaluation
-> Select Survivors (Elitism) [DE_PAIRWISE]
-> Optimization Monitor
-> Evolution Loop End
```

The **Adapt DE Parameters** node can be used to configure initial `F` and `CR` values and the adaptation probabilities for each parameter.

### 4.7. SHADE

**SHADE** adapts `F` and `CR` using a memory of parameter values that produced improvements during previous generations. Candidate generation generally uses the `current-to-pbest/1` strategy and an external archive of replaced parents.

Reference structure:

```text
Create Initial Population
-> initial evaluation
-> Finalize Evaluation
-> Evolution Loop Start
-> Adapt DE Parameters [SHADE]
-> Differential Evolution [CURRENT_TO_PBEST_1]
-> trial-candidate evaluation
-> Finalize Evaluation
-> Select Survivors (Elitism) [DE_PAIRWISE]
-> Update Archive [FIFO_UNIQUE]
-> Optimization Monitor
-> Evolution Loop End
```

The archive can be connected back to the optional input of **Differential Evolution** to increase the diversity of donor vectors.

### 4.8. L-SHADE

**L-SHADE** extends SHADE with a linear population-size reduction over the evaluation budget.

The recipe uses:

- success-history adaptation of `F` and `CR`;
- the `current-to-pbest/1` strategy;
- an archive of replaced parents;
- linear reduction between an initial and a minimum population size.

Reference structure:

```text
Create Initial Population
-> initial evaluation
-> Finalize Evaluation
-> Evolution Loop Start
-> Adapt DE Parameters [SHADE]
-> Differential Evolution [CURRENT_TO_PBEST_1]
-> trial-candidate evaluation
-> Finalize Evaluation
-> Select Survivors (Elitism) [DE_PAIRWISE]
-> Update Archive [FIFO_UNIQUE]
-> Reduce Population Size
-> Optimization Monitor
-> Evolution Loop End
```

The **Reduce Population Size** node uses the number of evaluations already consumed to compute the expected population size. Its parameters must be consistent with the initial size and the budget defined in **Experiment Setup**.

### 4.9. Summary table

| Algorithm | Family | Main representation | Number of objectives | Adaptation | Recipe available |
| --- | --- | --- | --- | --- | --- |
| GA | Evolutionary algorithm | Real-valued, integer, or binary | One objective in the reference recipe | Configurable operators | Yes |
| NSGA-II | Multi-objective evolutionary algorithm | Real-valued, integer, or binary depending on the operators | Two or more | Pareto rank and crowding | Yes |
| NSGA-III | Many-objective evolutionary algorithm | Real-valued, integer, or binary depending on the operators | Three or more | Reference directions | Yes |
| DE | Differential Evolution | Real-valued | One objective in the reference recipe | Fixed `F` and `CR` | Yes |
| GDE3 | Multi-objective DE | Real-valued | Two or more | Dominance-based selection | Yes |
| jDE | Self-adaptive DE | Real-valued | One objective | Individual-level `F` and `CR` | Yes |
| SHADE | Adaptive DE | Real-valued | One objective | Success-history memory and archive | Yes |
| L-SHADE | Adaptive DE | Real-valued | One objective | SHADE and linear population reduction | Yes |

---

## 5. Available nodes

IdeaFlow currently registers twenty-seven nodes in the KNIME Node Repository. They are grouped under the `Setup`, `Evolution`, `Evaluation`, `Results`, `Advanced`, and `Advanced / Utilities` categories.

```text
IDEAFlow
├── Setup
├── Evolution
├── Evaluation
├── Results
└── Advanced
    └── Utilities
```

<!-- Planned image: screenshot of the complete IDEAFlow category in the Node Repository. -->
<!-- ![Available IdeaFlow nodes](images/ideaflow-available-nodes.png) -->

### 5.1. Setup

#### 5.1.1. Experiment Setup

**Experiment Setup** creates the experiment plan. It produces one row per independent run and associates identifiers and a deterministic seed with each run.

Main parameters:

- experiment identifier;
- problem identifier;
- algorithm or recipe name;
- number of replications;
- master seed;
- maximum evaluation budget.

**Output:** `Run plan`.

#### 5.1.2. Define Optimization Problem

**Define Optimization Problem** describes the variables, objectives, and constraints of the problem.

The dialog can be used to create:

- continuous variables;
- integer variables;
- direct binary variables;
- numbered variable groups;
- groups of numerical values encoded using natural binary or Gray code;
- one or more objectives, each associated with a minimization or maximization direction;
- constraint-violation columns;
- optional hypervolume reference values.

**Output:** problem-definition table.

#### 5.1.3. Create Initial Population

**Create Initial Population** combines the experiment plan and problem definition to create an unevaluated population for each run.

Main parameters:

- population size;
- population identifier.

**Inputs:** `Experiment setup`, `Problem`.

**Output:** `Initial population`.

### 5.2. Evolution

#### 5.2.1. Evolution Loop Start

**Evolution Loop Start** starts a native IdeaFlow evolutionary loop. During the first iteration, it receives the already evaluated initial population. During subsequent iterations, it retrieves the population returned internally by **Evolution Loop End**.

**Input:** `Initial population`.

**Output:** `Current population`.

#### 5.2.2. Select Parents

**Select Parents** samples the individuals used by the crossover operator.

Available strategies:

- tournament;
- random selection.

Tournament selection can compare a scalar fitness value or automatically use Pareto-rank and crowding columns when they are available.

Main parameters:

- objectives and directions;
- strategy;
- tournament size;
- number of parents;
- selection with or without replacement;
- constraint-violation column.

#### 5.2.3. Crossover

**Crossover** combines selected parents to produce unevaluated offspring.

Available strategies:

- `SBX`;
- `UNIFORM`;
- `ONE_POINT`;
- `ARITHMETIC`.

Main parameters:

- variables to combine;
- evaluation results to invalidate;
- crossover probability;
- SBX distribution index;
- lower and upper bounds.

#### 5.2.4. Mutation

**Mutation** modifies the decision variables of candidates and invalidates outdated evaluation results.

Available strategies:

- polynomial mutation;
- Gaussian mutation;
- bit-flip mutation;
- random-reset mutation.

Main parameters:

- variables to mutate;
- evaluation results to invalidate;
- automatic rate of `1 / number of variables` or a manual probability;
- Gaussian mutation strength;
- polynomial distribution index;
- lower and upper bounds.

#### 5.2.5. Finalize Evaluation

**Finalize Evaluation** validates the columns computed by the evaluator and serves as the single boundary for counting exact evaluations.

The node:

- checks that objective columns are present;
- aggregates constraint violations;
- marks candidates as evaluated;
- updates internal evaluation and NFE columns;
- produces a portable history of newly completed evaluations.

**Input:** `Evaluation results`.

**Outputs:** `Evaluated population`, `Evaluation history`.

#### 5.2.6. Select Survivors (Elitism)

**Select Survivors (Elitism)** selects the population passed to the next generation from the evaluated parents and offspring.

Available modes:

- `SINGLE_OBJECTIVE`;
- `DE_PAIRWISE`;
- `NSGA_II`;
- `NSGA_III`;
- `GDE3`.

**Inputs:** `Current population`, `Evaluated children`.

**Outputs:** `Survivors`, `Rejected or replaced`.

The second output can notably feed a SHADE or L-SHADE archive.

#### 5.2.7. Evolution Loop End

**Evolution Loop End** receives the survivors, increments the generation, and returns the population to **Evolution Loop Start** until at least one stopping condition is met.

Available stopping conditions:

- maximum number of generations;
- maximum evaluation budget;
- optional target value for an objective.

**Input:** `Next population`.

**Final outputs:** `Final population`, `Run summary`.

The `Run summary` table notably contains the number of evaluations, the final generation, the best value, and the stopping reason.

### 5.3. Evaluation

#### 5.3.1. Evaluate Benchmark

**Evaluate Benchmark** computes the objectives of built-in test functions.

Available benchmarks:

- `ACKLEY`;
- `SPHERE`;
- `ROSENBROCK`;
- `RASTRIGIN`;
- `GRIEWANK`;
- `ONEMAX`;
- `ZDT1`;
- `ZDT2`;
- `ZDT3`;
- `DTLZ2`.

Main parameters:

- decision variables;
- produced objective columns;
- selected benchmark.

A custom problem can be evaluated using any KNIME nodes, provided that the result is then passed through **Finalize Evaluation**.

### 5.4. Results

#### 5.4.1. Optimization Monitor

**Optimization Monitor** passes the population through without modifying it and produces two monitoring tables:

- a summary per run and population;
- a detailed event per individual.

The summary notably contains:

- generation and NFE;
- population size;
- number of feasible individuals;
- best, mean, worst, and standard-deviation values;
- size of the non-dominated set;
- hypervolume when a reference point is provided.

**Outputs:** `Population`, `Progress summary`, `Detailed events`.

#### 5.4.2. Export Results

**Export Results** writes an optimization history in IOHprofiler format.

Produced files:

- `.info`;
- `.dat` for improvements;
- `.cdat` for the complete history, when enabled;
- an IdeaFlow TSV file preserving additional properties.

The node receives events produced by **Finalize Evaluation**, **Optimization Monitor**, or **Evolution Trace**.

### 5.5. Advanced

#### 5.5.1. Decode Binary Variables

**Decode Binary Variables** transforms genes created for a binary-encoded group into numerical variables that can be used directly by an evaluator.

Variable names, bounds, bit counts, and natural-binary or Gray encoding are read from the table produced by **Define Optimization Problem**.

**Inputs:** `Binary population`, `Problem`.

**Output:** `Decoded population`.

#### 5.5.2. Generate Reference Directions

**Generate Reference Directions** produces normalized Das-Dennis reference directions.

Main parameters:

- number of objectives;
- number of divisions.

These directions can be inspected, visualized, or reused in many-objective workflows.

#### 5.5.3. Differential Evolution

**Differential Evolution** creates the trial candidates used by DE, GDE3, jDE, SHADE, and L-SHADE.

Main parameters:

- continuous variables;
- fitness column and direction;
- differential mutation strategy;
- crossover strategy;
- `F`, `CR`, and p-best-rate parameters;
- bounds;
- repair method.

**Inputs:** `Current population`, optional `Archive`.

**Output:** `Trial candidates`.

#### 5.5.4. Update Archive

**Update Archive** manages two archive categories:

- `PARETO`, for non-dominated solutions;
- `FIFO_UNIQUE`, for replaced parents used by SHADE and L-SHADE.

The node can isolate archives per run or per run/population pair and limit their size.

**Inputs:** optional previous archive, new candidates, optional current population.

**Output:** updated archive.

#### 5.5.5. Adapt DE Parameters

**Adapt DE Parameters** adds or updates individual `F` and `CR` parameters.

Available modes:

- `FIXED`;
- `JDE`;
- `SHADE`.

Advanced parameters cover jDE adaptation probabilities and SHADE memory size.

#### 5.5.6. Reduce Population Size

**Reduce Population Size** applies a linear population-size reduction based on consumed NFE.

Main parameters:

- initial size;
- minimum size;
- evaluation budget;
- objective and direction used to remove the least competitive individuals.

#### 5.5.7. Surrogate Selection

**Surrogate Selection** ranks candidates already equipped with a prediction or acquisition score and separates:

- candidates that must be evaluated exactly;
- candidates retained as predictions.

The learning model remains external to the node and can be built using available KNIME components.

#### 5.5.8. Migrate Between Populations

**Migrate Between Populations** performs deterministic ring migration between several population identifiers within the same run.

Main parameters:

- fitness column;
- direction;
- number of migrants per island.

### 5.6. Advanced / Utilities

#### 5.6.1. Pareto Ranking & Diversity

**Pareto Ranking & Diversity** computes Pareto rank and crowding distance for an evaluated population while accounting for constraints.

#### 5.6.2. Iteration Controller

**Iteration Controller** manually increments the generation and produces a table of stopping decisions. It remains available for low-level workflows or architectures using generic KNIME loops. For standard workflows, the native **Evolution Loop Start** and **Evolution Loop End** nodes combine this logic.

#### 5.6.3. Quality Indicators

**Quality Indicators** computes the size of the feasible non-dominated set and exact dominated hypervolume for an arbitrary number of objectives.

#### 5.6.4. Reference Quality Indicators

**Reference Quality Indicators** compares an approximate front with a reference front.

Available indicators:

- GD;
- IGD;
- IGD+;
- additive epsilon;
- spacing.

#### 5.6.5. Population Statistics

**Population Statistics** produces one row per run, population, and generation containing population size, feasibility, best, mean, worst, and standard-deviation values.

These data can feed KNIME visualization and statistical-analysis nodes.

#### 5.6.6. Evolution Trace

**Evolution Trace** can be inserted after any stage manipulating a population. It passes the table through unchanged and produces events containing the stage name, operator name, and selected numerical values.

---

## 6. Using the extension: building a real-coded GA for Ackley

This section builds a real-coded genetic algorithm that minimizes the ten-dimensional Ackley function. The workflow serves as a reference introduction to IdeaFlow and covers the complete main cycle: problem definition, initialization, evaluation, selection, crossover, mutation, replacement, monitoring, and stopping.

The reserved path for the importable KNIME file corresponding to this tutorial is:

```text
examples/workflows/ackley-real-ga.knwf
```

<!-- When the workflow is added to the repository, insert the following relative link here: -->
<!-- [Open the tutorial Ackley workflow](examples/workflows/ackley-real-ga.knwf) -->

<!-- Planned image: complete screenshot of the final workflow, with annotations separating Setup, initial evaluation, evolution loop, and results. -->
<!-- ![Real-coded GA workflow for Ackley](images/ackley-real-ga-workflow.png) -->

### 6.1. Objective and reference parameters

The workflow minimizes Ackley using the following parameters:

| Parameter | Value |
| --- | --- |
| Benchmark | Ackley |
| Dimension | 10 |
| Bounds for each variable | `[-32.768, 32.768]` |
| Objective | Minimize `fitness` |
| Population size | 50 |
| Maximum budget | 10,000 exact evaluations |
| Master seed | 42 |
| Selection | Tournament of size 2 |
| Crossover | SBX, probability 0.9, index 20 |
| Mutation | Polynomial, automatic rate `1 / 10`, index 20 |
| Replacement | Single-objective `µ + λ` elitism |

### 6.2. Creating the workflow

Create a new KNIME workflow and give it an explicit name, for example:

```text
IdeaFlow Ackley Real GA
```

Then add the nodes described in the following sections from the `IDEAFlow` Node Repository.

### 6.3. Configuring Experiment Setup

Add **Experiment Setup** and enter:

| Field | Value |
| --- | --- |
| Experiment ID | `ackley-ga-tutorial` |
| Problem ID | `ackley-10d` |
| Algorithm or recipe | `ga.generational` |
| Replicates | `1` |
| Master seed | `42` |
| Maximum evaluations | `10000` |

A single replication is used in the tutorial to keep execution fast and easy to inspect. The same workflow can later be executed with multiple replications by changing only this parameter.

<!-- Planned image: Experiment Setup configuration dialog. -->
<!-- ![Experiment Setup configuration](images/ackley-experiment-setup.png) -->

### 6.4. Defining the optimization problem

Add **Define Optimization Problem**.

Configure the problem:

| Field | Value |
| --- | --- |
| Problem name | `ackley-10d` |

In the **Direct variables and groups** section, keep one group and enter:

| Field | Value |
| --- | --- |
| Name | `x` |
| Number of variables | `10` |
| First index | `0` |
| Kind of value | `Continuous number` |
| Smallest value | `-32.768` |
| Largest value | `32.768` |

The group produces columns `x0` through `x9`.

In the **Objectives** section, define:

| Field | Value |
| --- | --- |
| Result column | `fitness` |
| Optimization direction | `Minimize` |
| Use a hypervolume reference value | disabled |

No constraint is required for Ackley.

<!-- Planned image: Define Optimization Problem dialog showing the x group and the fitness objective. -->
<!-- ![Ackley problem definition](images/ackley-problem-definition.png) -->

### 6.5. Creating the initial population

Add **Create Initial Population** and connect:

```text
Experiment Setup -----------\
                             -> Create Initial Population
Define Optimization Problem /
```

Configure:

| Field | Value |
| --- | --- |
| Population size | `50` |
| Population ID | `population-0` |

The output contains fifty unevaluated individuals and the reserved `__if_` columns used by IdeaFlow for identity, generation, seeds, and evaluation accounting.

### 6.6. Evaluating and finalizing the initial population

Add **Evaluate Benchmark** after **Create Initial Population**.

Configure:

| Field | Value |
| --- | --- |
| Variable columns | `x0,x1,x2,x3,x4,x5,x6,x7,x8,x9` |
| Output objective columns | `fitness` |
| Benchmark | `ACKLEY` |

Then add **Finalize Evaluation** and select `fitness` under **Objective results**. Do not select any column under **Constraint results**.

The beginning of the workflow becomes:

```text
Experiment Setup + Define Optimization Problem
-> Create Initial Population
-> Evaluate Benchmark
-> Finalize Evaluation
```

**Finalize Evaluation** counts the fifty initial evaluations before the population enters the loop.

### 6.7. Adding the evolutionary loop

Add **Evolution Loop Start** after the first **Finalize Evaluation**.

Also add **Evolution Loop End** to the right of the future loop and configure it as follows:

| Field | Value |
| --- | --- |
| Stop after this many generations | `1000` |
| Stop after this many evaluations | `10000` |
| Objective used for stopping | `fitness` |
| Better values are | `Minimize` |
| Stop when a target is reached | disabled |

The generation limit is deliberately set above the required number of generations: the budget of 10,000 evaluations is the primary stopping criterion.

### 6.8. Selecting parents

Add **Select Parents** after **Evolution Loop Start**.

Configure:

| Field | Value |
| --- | --- |
| Objectives | `fitness` |
| Better values are | `Minimize` |
| Selection strategy | `Tournament` |
| Tournament size | `2` |
| Parents per population | `50` |
| Allow duplicate selections | enabled |
| Constraint violation column | `__if_constraint_violation` |

The number of parents equals the population size so that crossover produces fifty offspring per generation.

### 6.9. Applying crossover

Add **Crossover** after **Select Parents**.

Configure:

| Field | Value |
| --- | --- |
| Variables to combine | `x0` through `x9` |
| Evaluation results to clear | `fitness` |
| Strategy | `SBX` |
| Crossover probability | `0.9` |
| SBX distribution index | `20` |
| Smallest values | ten occurrences of `-32.768` |
| Largest values | ten occurrences of `32.768` |

Values to copy into the bound fields:

```text
-32.768,-32.768,-32.768,-32.768,-32.768,-32.768,-32.768,-32.768,-32.768,-32.768
```

```text
32.768,32.768,32.768,32.768,32.768,32.768,32.768,32.768,32.768,32.768
```

### 6.10. Applying mutation

Add **Mutation** after **Crossover**.

Configure:

| Field | Value |
| --- | --- |
| Variables to mutate | `x0` through `x9` |
| Evaluation results to clear | `fitness` |
| Strategy | `Polynomial` |
| Automatic mutation rate | enabled |
| Polynomial distribution index | `20` |
| Smallest values | ten occurrences of `-32.768` |
| Largest values | ten occurrences of `32.768` |

The automatic rate is `1 / 10`, corresponding to a mutation probability of `0.1` per variable.

<!-- Planned image: Crossover and Mutation dialogs shown side by side. -->
<!-- ![Variation operator configuration](images/ackley-variation-settings.png) -->

### 6.11. Evaluating offspring

Add a second **Evaluate Benchmark** after **Mutation**, using the same configuration as the initial evaluation:

```text
Benchmark: ACKLEY
Variables: x0 through x9
Output objective: fitness
```

Add a second **Finalize Evaluation** immediately afterward. Select `fitness` as the objective and define no constraint.

Each generation therefore adds fifty exact evaluations to the run NFE.

### 6.12. Selecting survivors

Add **Select Survivors (Elitism)**.

Create two connections:

```text
Evolution Loop Start -----------------------------> Current population
Offspring Finalize Evaluation --------------------> Evaluated children
```

Configure:

| Field | Value |
| --- | --- |
| Update mode | `Elitism - keep the best candidates` (`SINGLE_OBJECTIVE`) |
| Objectives | `fitness` |
| Better values are | `Minimize` |
| Constraint violation column | `__if_constraint_violation` |

The node keeps fifty individuals from the fifty parents and fifty evaluated offspring.

### 6.13. Adding monitoring and closing the loop

Add **Optimization Monitor** after **Select Survivors (Elitism)**.

Configure:

| Field | Value |
| --- | --- |
| Objectives to monitor | `fitness` |
| Better values are | `Minimize` |
| Hypervolume reference | empty |
| Recorded stage name | `generation` |

Connect the `Population` output of **Optimization Monitor** to **Evolution Loop End**.

The complete loop body is then:

```text
Evolution Loop Start
├── Current population ------------------------------┐
│                                                    v
└-> Select Parents -> Crossover -> Mutation        Select Survivors
                         -> Evaluate Benchmark         |
                         -> Finalize Evaluation        v
                                              Optimization Monitor
                                                       |
                                                       v
                                              Evolution Loop End
```

As long as the budget has not been exhausted, **Evolution Loop End** passes the survivor population back to **Evolution Loop Start** and begins the next generation.

### 6.14. Executing the workflow

Execute the complete workflow from KNIME. At the end of the loop, inspect:

- the `Final population` output of **Evolution Loop End**;
- the `Run summary` output;
- the **Optimization Monitor** view for the most recently computed summary;
- monitoring outputs when they are collected in an experimental version of the workflow.

The `Run summary` table should indicate a stopping reason related to the maximum evaluation budget and a best `fitness` value lower than those obtained in the initial population.

### 6.15. Preserving and visualizing a complete trajectory

The minimal workflow directly preserves the final population and stopping summary. To produce a complete convergence curve, the monitoring branch must collect summaries or events generated at each generation using KNIME table-collection mechanisms or an experimental sub-workflow.

The data to preserve notably include:

- `__if_generation`;
- `__if_nfe`;
- `best`;
- `mean`;
- `worst`;
- `standard_deviation`.

Once collected, these data can be connected to a KNIME **Line Plot** node, using NFE or generation on the x-axis and `best` on the y-axis.

---

## 7. Reference workflow: real-coded GA for Ackley

This section summarizes the workflow built in Section 6 and defines the expected contents of the example file published with IdeaFlow.

### 7.1. Problem overview

The Ackley function is a continuous multimodal benchmark. For a decision vector of dimension `d`, it has a global optimum of `0` when all variables are equal to `0`.

The reference workflow uses ten real-valued variables, `x0` through `x9`, each defined in the interval `[-32.768, 32.768]`, and produces a single objective column, `fitness`, to be minimized.

### 7.2. Algorithm parameters

| Parameter | Value |
| --- | --- |
| Recipe | `ga.generational` |
| Replications in the tutorial file | 1 |
| Master seed | 42 |
| Population size | 50 |
| Dimension | 10 |
| Evaluation budget | 10,000 |
| Maximum number of generations | 1,000 |
| Selection | Tournament, size 2, with replacement |
| Number of parents | 50 |
| Crossover | SBX |
| Crossover probability | 0.9 |
| SBX index | 20 |
| Mutation | Polynomial |
| Mutation probability | Automatic, `1 / d` |
| Mutation index | 20 |
| Replacement | Single-objective `µ + λ` elitism |
| Objective | `fitness`, minimization |

### 7.3. Overall structure

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
                                            |
                          Final population + Run summary
```

<!-- Planned image: final screenshot of the workflow reproducing exactly this structure. -->
<!-- ![Complete structure of the Ackley workflow](images/ackley-reference-workflow.png) -->

### 7.4. Node configuration

The complete configuration is described step by step in Section 6. The published file must preserve the reference parameters without relying on a local path, an external table, or a machine-specific environment variable.

Canvas annotations may distinguish four areas:

1. **Experiment and problem setup**;
2. **Initial evaluation**;
3. **Evolutionary loop**;
4. **Final results**.

This organization improves workflow readability without affecting execution.

### 7.5. Expected execution behavior

Execution is deterministic for a given extension version, identical seed, and identical parameters. The exact numerical result may change if operator implementations are modified, but the following properties must remain verifiable:

- the initial population contains fifty individuals;
- variables respect the Ackley bounds;
- each evaluated candidate has a `fitness` value;
- NFE increases only after **Finalize Evaluation**;
- each generation produces fifty evaluated offspring;
- the survivor population remains at fifty individuals;
- the loop stops at the budget of 10,000 evaluations or at a more restrictive explicitly configured limit;
- the final best fitness is less than or equal to the initial best fitness because of elitist replacement.

### 7.6. Result analysis

The `Final population` output can be used to inspect:

- final coordinates `x0` through `x9`;
- the identifier of each individual;
- its generation and evaluation history;
- its `fitness` value.

The `Run summary` output provides:

- the corresponding run and population;
- the final generation;
- reached NFE;
- stopping state;
- stopping reason;
- best objective value.

<!-- Planned image: screenshot of the Run summary table and several rows of Final population. -->
<!-- ![Ackley workflow results](images/ackley-results.png) -->

When the complete trajectory is collected, a convergence curve can display best fitness as a function of NFE. This curve should be interpreted as an execution-monitoring tool; a scientific comparison between algorithms requires multiple independent runs and an appropriate statistical analysis.

### 7.7. Publishing the workflow in the repository

The reference file must be exported from KNIME and added to the repository at:

```text
examples/workflows/ackley-real-ga.knwf
```

The relative link to display in the README will then be:

```markdown
[Open the tutorial Ackley workflow](examples/workflows/ackley-real-ga.knwf)
```

The directory may also contain a screenshot of the canvas and a short file documenting the IdeaFlow and KNIME versions used for the export.

---

---

## 8. Reusing and encapsulating workflows

Sub-workflows are a native KNIME feature. They can be used to encapsulate a complete workflow or part of an experimental protocol and then call it from another workflow. IdeaFlow relies on this mechanism to separate algorithm definition, execution, and result analysis.

### 8.1. Sub-workflow principle

A sub-workflow is a KNIME workflow saved independently and exposing input and output ports. The calling workflow passes one or more tables to it, triggers its execution, and then retrieves the produced tables.

In IdeaFlow, a sub-workflow may contain:

- a complete algorithm;
- a specific algorithm variant;
- a custom evaluation function;
- a post-processing pipeline;
- a monitoring or export protocol.

### 8.2. Use cases

Encapsulation is particularly useful for:

- keeping the main workflow readable;
- reusing exactly the same configuration across several experiments;
- comparing several recipes from the same experiment plan;
- isolating a costly evaluation or one that depends on external resources;
- sharing an algorithm without immediately exposing all its details on the main canvas.

It also makes it possible to distinguish two levels: the algorithmic workflow, which transforms a population, and the experimental workflow, which organizes replications, comparisons, statistics, and exports.

### 8.3. Defining inputs and outputs

Ports are defined using KNIME nodes designed for callable workflows, notably:

```text
Container Input (Table)
Container Output (Table)
```

Depending on its role, an IdeaFlow sub-workflow may receive:

- an experiment plan;
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

### 8.4. Calling a sub-workflow

The main workflow can call a saved workflow using:

```text
Call Workflow (Table Based)
```

This node must be configured to point to the target workflow and map its ports to the tables of the main workflow. The called workflow must remain accessible from the environment in which the experiment is executed.

<!-- Planned image: main workflow containing Call Workflow, followed by a screenshot of the called workflow. -->
<!-- ![Calling an IdeaFlow sub-workflow](images/ideaflow-call-workflow.png) -->

### 8.5. Example: encapsulating the Ackley GA

The workflow built in Sections 6 and 7 can be encapsulated as a reusable algorithm. One possible organization is to expose:

**Inputs:**

- the plan produced by **Experiment Setup**;
- the definition produced by **Define Optimization Problem**.

**Outputs:**

- `Final population`;
- `Run summary`;
- events or statistics collected during execution.

The main workflow can then provide several experiment-plan rows, call the Ackley GA, and aggregate the results of all replications.

<!-- Planned image: screenshot of the Ackley GA sub-workflow after it is created. -->
<!-- ![Ackley GA sub-workflow](images/ackley-ga-subworkflow.png) -->

### 8.6. Advantages and limitations

Sub-workflows improve modularity, reusability, and readability. They also simplify comparison protocols because the same main workflow can call several independently configured algorithms.

However, they introduce several constraints:

- the called workflow and its resources must be available;
- port and column schemas must be stable;
- the recipient must use a compatible IdeaFlow version;
- excessive encapsulation can hide important stages of the algorithm;
- local paths, external files, and environment variables should be avoided in workflows intended for sharing.

---

## 9. Developing and extending IdeaFlow

This section presents the architecture required to add new strategies or nodes. It is intended for contributors with a Java and KNIME development environment.

### 9.1. Source-code organization

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

Implementations should place algorithmic logic in `core` or behind an interface from `spi` whenever possible, while classes in `nodes` should be limited to configuration, table conversion, and invocation of this logic.

<!-- Planned image: package tree in the IDE. -->
<!-- ![IdeaFlow source-code organization](images/ideaflow-source-tree.png) -->

### 9.2. Adding a strategy or adding a node

Two forms of extension must be distinguished.

**Adding a strategy to an existing node** is appropriate when the operation already has the same inputs, outputs, and responsibilities. For example, a new bound-repair, ranking, or migration strategy can be integrated behind an SPI interface without creating a new node on the canvas.

**Adding a new KNIME node** is necessary when the operation introduces a new role in the workflow, a new port schema, or configuration that does not match any existing node.

This distinction prevents node proliferation when only the internal method changes.

### 9.3. General structure of a KNIME node

A Java node generally consists of:

- a `NodeFactory`, responsible for creating the node components;
- a `NodeModel`, which defines ports, validates tables, and executes the operation;
- a configuration dialog;
- an XML file describing the node and its help content;
- a declaration in `plugin.xml` to register the node in the Node Repository.

The `NodeModel` must notably:

1. declare its inputs and outputs precisely;
2. validate the required columns;
3. reject inconsistent configurations with an explicit message;
4. preserve IdeaFlow columns not modified by the operation;
5. produce a table whose schema remains stable between configuration and execution.

### 9.4. Creating a new operator strategy

#### 9.4.1. Defining the contract

Before implementation, specify:

- the algorithmic role of the strategy;
- compatible representations;
- the accepted number of objectives;
- required columns;
- produced or modified properties;
- behavior regarding constraints and seeds.

#### 9.4.2. Reusing an SPI interface

The `org.ideaflow.v2.spi` package contains contracts intended for replaceable strategies, including selection, variation, population update, ranking, dominance, archives, stopping criteria, quality indicators, bound repair, and migration topologies.

When an existing interface matches the new operator, the implementation should respect this contract instead of introducing a parallel API.

#### 9.4.3. Declaring the strategy

Dynamically discovered strategies are registered through the Java `ServiceLoader` mechanism. The fully qualified name of the implementation class must be added to the corresponding file under:

```text
META-INF/services/
```

A strategy must expose a stable identifier and a description of its capabilities so that it can be selected and validated by the components that use it.

#### 9.4.4. Exposing parameters

If the existing node can host the strategy, its dialog must allow users to select the strategy and enter only the relevant parameters. Non-applicable parameters should be disabled or explicitly ignored.

Every new configuration key must be:

- named consistently and remain stable;
- saved and restored with the workflow;
- validated before execution;
- documented in the node XML file.

#### 9.4.5. Preserving reproducibility

A strategy using randomness must derive its random draws from the seeds associated with the run and candidates. It must not create an uncontrolled generator that would make otherwise identical executions non-reproducible.

#### 9.4.6. Testing the strategy

Tests must cover at least:

- the nominal case;
- small populations;
- boundary parameter values;
- bounds and constraints;
- reproducibility with a fixed seed;
- invalid data;
- integration in a complete workflow.

### 9.5. Creating a new visible node

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

### 9.6. Validation before contributing

Before proposing a change:

- build the plugin and feature;
- run the core tests;
- run export tests when relevant;
- open KNIME with the updated plugin;
- create or update a test workflow;
- verify that the workflow can be saved and reopened;
- update the documentation and changelog.

---

## 10. Creating a new algorithmic recipe

In IdeaFlow, an algorithm is primarily represented by a composition of nodes and strategies. Creating a new recipe therefore does not systematically require a new node.

### 10.1. Defining the algorithm cycle

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

### 10.2. Identifying the required strategies

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

### 10.3. Developing the missing elements

Missing elements must be added according to the distinction presented in Section 9: a new strategy behind an existing SPI, or a new node when the operation introduces a new role in the workflow.

Columns produced by a stage must be documented, especially when they are required by the following stage.

### 10.4. Adding the declarative recipe

Built-in recipes are described under:

```text
org/ideaflow/v2/recipes/
```

A new recipe must define a stable identifier, the expected strategies, and compatibility constraints useful for its validation. It must also be added to the recipe index when this index is used during loading.

The recipe describes a reference configuration; it does not replace the KNIME workflow that concretely shows the node sequence.

### 10.5. Building the reference workflow

The reference workflow must:

- use the nodes corresponding to the recipe;
- make essential stages visible;
- define consistent and documented parameters;
- avoid any dependency on a local path;
- use a deterministic seed;
- produce a final population and an execution summary;
- preserve results that can be used for testing.

### 10.6. Testing on a suitable benchmark

The benchmark must match the algorithm domain:

- `ONEMAX` for a binary recipe;
- `SPHERE`, `ACKLEY`, `RASTRIGIN`, `ROSENBROCK`, or `GRIEWANK` for a continuous single-objective method;
- `ZDT1`, `ZDT2`, or `ZDT3` for a multi-objective method;
- `DTLZ2` for a many-objective method.

Tests should verify the workflow structure and algorithm invariants without depending on one unique final value when the method is stochastic.

### 10.7. Documenting the recipe

The documentation must specify:

- algorithm family;
- compatible representations;
- number of objectives;
- used nodes and strategies;
- main parameters;
- reference workflow;
- known limitations;
- useful bibliographic references.

---

## 11. Surrogate-assisted optimization

IdeaFlow does not currently include a mandatory built-in machine-learning model. It does, however, provide **Surrogate Selection**, a coordination node capable of using predictions produced by KNIME learning nodes or by an external integration.

### 11.1. Principle

A surrogate model approximates an expensive evaluation function from previously evaluated candidates. It can predict fitness, uncertainty, or an acquisition score in order to reduce the number of exact evaluations.

The general flow is:

```text
proposed candidates
-> KNIME predictive model
-> prediction / uncertainty / acquisition
-> Surrogate Selection
├── candidates selected for exact evaluation
└── deferred candidates or candidates retained as predictions
```

### 11.2. Role of Surrogate Selection

The node ranks candidates that already have a prediction or score column. It separates candidates selected for exact evaluation from those that are not evaluated immediately.

The model, its training, validation, and retraining remain the responsibility of the KNIME workflow. IdeaFlow does not alter the predicted value and counts an exact evaluation only when the candidate passes through **Finalize Evaluation**.

### 11.3. Limitations

Surrogate-assisted optimization must account for:

- model bias and error;
- the amount of exact data available;
- retraining cost;
- uncertainty management;
- maintaining a balance between exploration and exploitation;
- the risk of converging toward a region incorrectly favored by the approximation.

This feature is an integration point with the KNIME machine-learning ecosystem, not a machine-learning algorithm provided by IdeaFlow.

---

## 12. Advanced example: building L-SHADE

L-SHADE combines Differential Evolution, success-history adaptation of `F` and `CR`, an archive of replaced parents, and linear population-size reduction.

### 12.1. Differences from standard DE

A standard DE recipe generally uses fixed `F` and `CR` values and keeps a constant population size. L-SHADE adds:

- a memory of parameter values that produced improvements;
- individual sampling of `F` and `CR`;
- the `current-to-pbest/1` strategy;
- an external archive used during donor-vector generation;
- progressive population reduction based on the consumed budget.

### 12.2. Required nodes

The workflow uses at least:

```text
Experiment Setup
Define Optimization Problem
Create Initial Population
Evaluate Benchmark or custom evaluation
Finalize Evaluation
Evolution Loop Start
Adapt DE Parameters
Differential Evolution
Finalize Evaluation
Select Survivors (Elitism)
Update Archive
Reduce Population Size
Optimization Monitor
Evolution Loop End
```

### 12.3. Workflow structure

```text
initial population
-> exact evaluation
-> Finalize Evaluation
-> Evolution Loop Start
-> Adapt DE Parameters [SHADE]
-> Differential Evolution [CURRENT_TO_PBEST_1]
-> exact evaluation
-> Finalize Evaluation
-> Select Survivors [DE_PAIRWISE]
├── survivors -> Reduce Population Size -> Monitor -> Loop End
└── replaced parents -> Update Archive
                           |
                           +-> archive reconnected to Differential Evolution
```

<!-- Planned image: screenshot of the L-SHADE workflow showing the archive feedback loop. -->
<!-- ![L-SHADE workflow](images/lshade-workflow.png) -->

### 12.4. Configuring parameter adaptation

In **Adapt DE Parameters**:

- select `SHADE` mode;
- define initial `F` and `CR` values;
- choose the memory size;
- preserve the produced parameter columns for the variation node.

The node must be executed before **Differential Evolution** at each generation.

### 12.5. Configuring Differential Evolution

In **Differential Evolution**:

- select the continuous variables;
- choose `CURRENT_TO_PBEST_1`;
- choose binomial crossover or the crossover specified by the recipe;
- enter the p-best rate;
- define bounds and the repair policy;
- connect the archive produced by **Update Archive** to the corresponding optional input.

### 12.6. Updating the archive

The second output of **Select Survivors (Elitism)** contains rejected or replaced parents. It feeds **Update Archive** in `FIFO_UNIQUE` mode.

The archive must be preserved between generations and connected back to **Differential Evolution**. Its maximum size must remain consistent with the population and the selected recipe.

### 12.7. Reducing the population

Configure **Reduce Population Size** with:

- the initial size;
- the minimum size;
- the maximum evaluation budget;
- the objective and direction used to remove the least competitive individuals.

These values must be consistent with **Experiment Setup** and **Create Initial Population**. Reduction is computed from the NFE actually counted by **Finalize Evaluation**.

### 12.8. Executing and verifying

During a valid execution:

- `F` and `CR` are present for the relevant candidates;
- the archive is populated with replaced parents;
- its output is reused during the next generation;
- population size decreases progressively;
- NFE increases only after exact evaluations;
- the maximum budget stops the loop.

An exportable L-SHADE workflow can be added to the repository after it has been validated in KNIME.

---

## 13. Best practices

### 13.1. Naming nodes and workflow areas

Rename node instances when several nodes of the same type are present, for example `Initial Evaluation` and `Offspring Evaluation`. Use annotations to clearly separate setup, initial evaluation, loop, results, and export areas.

### 13.2. Keeping the data flow readable

Prefer a left-to-right reading direction, limit crossing connections, and use vertical branches for auxiliary data such as problem definitions, archives, or reference directions.

### 13.3. Separating the algorithm from the problem

Evolutionary logic should remain independent from the evaluation function. For a custom problem, produce objective and constraint columns using KNIME nodes, then use **Finalize Evaluation** as the explicit boundary between evaluation and evolution.

### 13.4. Preserving internal columns

Columns prefixed with `__if_` maintain identity, seeds, evaluation state, NFE, generation, and population management. They must be preserved unless a documented IdeaFlow node is responsible for modifying them.

### 13.5. Fixing seeds and preserving parameters

Use **Experiment Setup** to define replications and the master seed. Preserve the following information with the results:

- IdeaFlow version;
- KNIME version;
- benchmark or problem definition;
- operator parameters;
- budget;
- seeds;
- stopping criteria.

### 13.6. Using Finalize Evaluation once per exact evaluation

An evaluation should be counted only after its objectives and constraints have been produced. Omitting **Finalize Evaluation** prevents correct NFE updates; placing it several times on the same results may distort the budget.

### 13.7. Exporting reproducible data

Use **Optimization Monitor**, **Population Statistics**, **Evolution Trace**, and **Export Results** according to the required level of detail. Exported files should not depend on a personal path when intended for sharing.

### 13.8. Validating each recipe with a reference workflow

Every documented recipe should have an importable workflow, a known seed, and verifiable invariants. Stochastic results should be analyzed over multiple replications when conducting a scientific comparison.

---

## 14. Troubleshooting

### 14.1. A node cannot be configured

Verify that preceding nodes are configured and that the expected columns exist in their output schemas. Some dialogs can only offer variable or objective columns after upstream nodes have been configured.

### 14.2. A variable or objective cannot be found

Verify the names defined in **Define Optimization Problem** and those produced by the evaluation. Names are sensitive to spelling differences. A column declared as an objective must exist before **Finalize Evaluation**.

### 14.3. Candidates remain unevaluated

Verify that:

- the evaluation function produces the expected columns;
- **Finalize Evaluation** is placed immediately after evaluation;
- objectives are selected in its dialog;
- produced values are numerical and valid.

### 14.4. NFE does not increase correctly

NFE is updated by **Finalize Evaluation**. Verify that it is executed once for each newly evaluated batch and that an already finalized population is not finalized a second time without a new evaluation.

### 14.5. The loop does not stop

Verify the configuration of **Evolution Loop End**: maximum number of generations, evaluation budget, and optional target. The budget can only be reached if NFE is updated correctly.

### 14.6. Select Survivors produces an error

Verify that:

- the first input contains the current population;
- the second contains evaluated offspring;
- objectives and directions are consistent;
- the selected mode matches the number of objectives;
- run and population identifiers are preserved;
- columns specific to DE or multi-objective optimization are available when required by the mode.

### 14.7. Results change despite using a fixed seed

Verify that all random operations use IdeaFlow seeds and that no external node introduces an uncontrolled generator. Parallelization, an external model, or a changing data source may also modify the result.

### 14.8. The SHADE or L-SHADE archive remains empty

The second output of **Select Survivors (Elitism)** must feed **Update Archive**. The node must use `FIFO_UNIQUE` mode, and its output must be preserved between generations and connected back to **Differential Evolution**.

### 14.9. A sub-workflow does not receive the correct columns

Compare the schemas of `Container Input (Table)` and `Container Output (Table)` ports with those expected by the calling workflow. Also verify that internal `__if_` columns have not been filtered out.

### 14.10. The extension no longer loads after an update

For a manual installation, remove older JAR versions from `dropins`, place the compatible version there, and restart KNIME. Also verify the KNIME version and Java environment being used.

---

## 15. FAQ

### 15.1. Can IdeaFlow be used without programming?

Yes. Available recipes and operators can be assembled and configured graphically. Developing a new strategy, node, or Java function, however, requires software-development skills.

### 15.2. Can a custom optimization problem be defined?

Yes. The workflow can use any KNIME nodes to compute objectives and constraints. Produced columns must then be validated by **Finalize Evaluation**.

### 15.3. Does IdeaFlow support multiple objectives?

Yes. The NSGA-II, NSGA-III, and GDE3 recipes, Pareto-ranking tools, archives, and quality indicators can be used to build multi-objective and many-objective workflows.

### 15.4. Can several recipes be combined?

The nodes are composable, which makes it possible to create variants or hybrid architectures. The combination must nevertheless preserve a coherent cycle, compatible schemas, and a clearly defined population-update mechanism.

### 15.5. Can Python, R, or JavaScript be used in the workflow?

Yes, through the corresponding KNIME integrations. IdeaFlow's core remains implemented in Java, but populations are represented as KNIME tables that can be processed by other nodes. Internal columns and the finalization protocol must be preserved.

### 15.6. Does IdeaFlow directly provide a machine-learning model?

No. **Surrogate Selection** can use a prediction or score generated by an external model, but model training and application are performed using KNIME tools or another integration.

### 15.7. Can results be exported?

Yes. **Export Results** produces IOHprofiler files and an additional TSV export. Tabular outputs can also be saved using standard KNIME writer nodes.

### 15.8. Can an IdeaFlow workflow be shared?

Yes. The recipient must use compatible KNIME and IdeaFlow versions and have access to any external files or resources referenced by the workflow.

### 15.9. Are executions reproducible?

IdeaFlow provides deterministic seeds per run. Full reproducibility, however, also requires stabilizing external nodes, data, software versions, and used resources.

---

## 16. Conclusion

IdeaFlow provides a native KNIME architecture for representing population-based metaheuristics as composable workflows. The current version covers GA, NSGA-II, NSGA-III, DE, GDE3, jDE, SHADE, and L-SHADE, together with the mechanisms required for custom evaluation, monitoring, archives, multi-objective indicators, reproducible experiments, and result export.

The Ackley tutorial demonstrates the complete cycle of a real-coded genetic algorithm. The sections on sub-workflows, code extension, and advanced recipes explain how this architecture can be reused for more complex experiments.

### 16.1. Current limitations

The analyzed version does not provide a reference recipe for CMA-ES, PSO, or ACO. It also does not provide an integrated machine-learning model: surrogate coordination relies on external models available in KNIME.

The importable KNIME workflows presented in this documentation must be added to the repository as they are validated. Official publication of the extension on a KNIME update site follows a process separate from manual JAR distribution.

### 16.2. Repository resources

Exact paths may be adapted to the final organization of the repository. The documentation can notably be distributed across:

```text
README.md
docs/INSTALLATION.md
docs/WORKFLOWS.md
docs/DEVELOPMENT.md
docs/PUBLICATION.md
examples/workflows/
CHANGELOG.md
LICENSE
```

The Ackley workflow described in this tutorial must be published at:

```text
examples/workflows/ackley-real-ga.knwf
```
