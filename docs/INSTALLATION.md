# Installing IdeaFlow

[Main README](../README.md) · [Documentation index](README.md) · [Overview](OVERVIEW.md) · [KNIME installation](KNIME_INSTALLATION.md) · [IdeaFlow installation](INSTALLATION.md) · [Node reference](NODES.md) · [Optimization problems](OPTIMIZATION_PROBLEMS.md) · [Workflow tutorial](WORKFLOW_TUTORIAL.md) · [Development](DEVELOPMENT.md) · [Troubleshooting](TROUBLESHOOTING.md)


> [!NOTE]
> This document covers the [IdeaFlow extension](https://github.com/dpallez/IdeaFlow) itself. For [KNIME Analytics Platform](https://www.knime.com/knime-analytics-platform) installation on Windows, macOS, and Linux, including optional Python, Conda, Pixi, pip, and R setup, see the [general KNIME installation guide](KNIME_INSTALLATION.md).

This section explains how to install the IdeaFlow extension in KNIME and verify that its nodes are available in the Node Repository.

> **Distribution status:** until publication on a KNIME update site is finalized, the manual installation described in Section 3.4 is the reference testing method.

The recommended method is to install IdeaFlow directly from KNIME when the extension is available as an add-on or through an update site. Depending on the selected distribution method, the extension may also be obtained from a project website, for example through an installation link, archive, or `.jar` file.

Manual installation remains possible by placing the extension `.jar` file directly in KNIME's `dropins` directory. This method is useful for testing a local or unpublished version, but it is less convenient for updates than installation through KNIME.

<!-- Planned image: screenshot of KNIME showing how to access the extension installation menu. -->

<!-- Example: ![Installing the extension](images/install_extension_menu.png) -->

## Requirements

Before installing the extension, a compatible KNIME environment is required.

The required elements generally include:

* a compatible version of KNIME Analytics Platform;
* a functional KNIME workspace;
* optionally, access to the website on which the extension is published;
* optionally, the extension `.jar` file for manual installation.

In a standard installation, end users do not normally need to configure Java themselves because KNIME provides its own runtime environment. However, the Java version remains important for development and compatibility because the extension is developed in Java.

It is recommended to check the KNIME version before installation, especially while the extension is still under development or distributed as a test version.

### Required KNIME version

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

### Required Java version

Because IdeaFlow is developed with [Java 21](https://openjdk.org/projects/jdk/21/), its compatibility depends on the Java environment used by KNIME.

End users generally do not need to install Java separately if KNIME already provides its own runtime environment. This information mainly indicates the version used to develop and test the extension.

```text
Recommended Java version: JDK 21
Tested version: JDK 21
```

If the extension fails to load, it may be useful to verify that the installed KNIME version is compatible with the Java version targeted by the project.

### Required dependencies

```text
No external dependency needs to be installed manually.
```

## Installation from KNIME

The recommended method is to install IdeaFlow directly from KNIME using the [extension installation mechanism](https://docs.knime.com/ap/latest/analytics_platform_installation_guide/#installing-extensions-and-integrations).

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

## Installation from the project website or an update site

When an [Eclipse/KNIME update site](https://docs.knime.com/ap/latest/analytics_platform_installation_guide/#installing-extensions-and-integrations) is provided, it must first be added to KNIME's list of known software sites.

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


## Manual installation using a `.jar` file

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

## Verifying the installation

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
Selection
```

If the installation is successful, the extension nodes should appear in the Node Repository.

<!-- Planned image: screenshot of the Node Repository with "IdeaFlow" in the search field. -->

<!-- Example: ![Searching for IdeaFlow in the Node Repository](images/search_ideaflow_node_repository.png) -->

<!-- Planned image: screenshot of an empty workflow with a first IdeaFlow node placed on the canvas. -->

<!-- Example: ![First IdeaFlow node in a workflow](images/first_ideaflow_node.png) -->

## Node location in KNIME

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
- `Loops`;
- `Operators`;
- `Evaluation`;
- `Multi-Objective`;
- `Multiple Populations`;
- `Analysis`;
- `Export`.

<!-- Planned image: screenshot of the full category containing IdeaFlow nodes. -->

<!-- Example: ![Location of IdeaFlow nodes](images/ideaflow_nodes_location.png) -->

## Common installation issues

This section lists the most common problems that may occur while installing the extension.

### The extension does not appear

If the extension does not appear in KNIME after installation, verify that:

* KNIME was restarted after installation;
* the installed KNIME version is compatible with the extension;
* the update site or installation files are correct;
* the installation was not interrupted;
* the `.jar` file was placed in the `dropins` directory for a manual installation;
* no error message appeared in the console or logs.

It may also be useful to search directly for the name of one of the nodes in the Node Repository.

For a manual `.jar` installation, also verify that an older version of the `.jar` file is not still present in the `dropins` directory, as multiple versions may conflict.

### Nodes do not execute

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

### Java version issue

An incompatible Java version may prevent the extension from loading or executing.

If a Java-related error appears, verify:

* the Java version used by KNIME;
* the Java version used to develop the extension;
* compatibility between both versions;
* the runtime environment configuration.

```text
Recommended Java version: JDK 21
```

### Dependency issue

When a dependency is missing, some nodes may fail to appear or execute correctly.

In this case, verify that all required libraries are included in the extension or installed in the KNIME environment.

When the extension is distributed as a complete package, dependencies should normally be included in the installation.

### Manual installation issue

When installing manually from a `.jar` file, several issues may occur:

* the file was not placed in the correct `dropins` directory;
* KNIME was not restarted after the file was added;
* an older `.jar` version is still present;
* the `.jar` file was not built correctly;
* the extension is incompatible with the installed KNIME version.

Whenever possible, prefer installation from KNIME or an update site for officially distributed versions.

---
## Related documentation

- [General KNIME installation](KNIME_INSTALLATION.md)
- [KNIME extension installation guide](https://docs.knime.com/ap/latest/analytics_platform_installation_guide/#installing-extensions-and-integrations)
- [Node reference](NODES.md)
- [Troubleshooting](TROUBLESHOOTING.md)
- [IdeaFlow repository](https://github.com/dpallez/IdeaFlow)
