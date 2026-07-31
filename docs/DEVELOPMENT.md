# Developing and Extending IdeaFlow

[Documentation index](README.md) · [Testing](TESTING.md) · [Jenkins](JENKINS.md) · [Node reference](NODES.md)

IdeaFlow is an Eclipse/OSGi plugin built with Maven Tycho for KNIME Analytics Platform. Production code is in `ideaflow-knime/IDEAFlow`, tests are in `ideaflow-knime/IDEAFlow.tests`, and the p2 feature and update site have their own reactor modules.

## Package organization

| Package | Responsibility |
| --- | --- |
| `org.ideaflow.api` | Public candidates, problem definitions, run state, events, reserved columns, and the IdeaFlow state cell |
| `org.ideaflow.core` | Deterministic algorithms and utilities independent of node dialogs and KNIME table conversion |
| `org.ideaflow.io` | IOHprofiler serialization |
| `org.ideaflow.knime` | Shared KNIME table, metadata, parameter, and evaluation support |
| `org.ideaflow.nodes` | Visible node implementations, organized by node responsibility |
| `org.ideaflow.spi` | Public contracts for replaceable strategies and state codecs |

Each visible node has its own package when its implementation contains several classes. A node package normally contains its factory, model, dialog or modern parameter class, and XML help descriptor. Shared code belongs in `core` or `knime`, not in an unrelated node package.

## Choosing between a strategy and a node

Add a strategy to an existing node when the operation has the same workflow role, ports, and lifecycle. Add a visible node when the operation introduces a genuinely different responsibility, port schema, or user interaction. This keeps workflows understandable without creating a separate node for every algorithm variation.

## Service-provider extensions

IdeaFlow discovers replaceable implementations with Java `ServiceLoader`. Public interfaces currently cover bounds repair, dominance comparison, ranking, migration topology, quality indicators, and state codecs.

To add an implementation:

1. implement the appropriate interface from `org.ideaflow.spi`;
2. give it a stable identifier and capability description;
3. add its fully qualified class name to the matching file under `src/META-INF/services`;
4. keep exactly one descriptor for each service interface;
5. add discovery and behavior tests.

Randomized strategies must derive their randomness from IdeaFlow's run and candidate seeds.

## Adding a visible node

1. Create a responsibility-specific package below `org.ideaflow.nodes`.
2. Implement the node factory and model, plus its dialog or modern parameters.
3. Add the `NodeFactory.xml` help descriptor when required by the factory style.
4. Use the shared `default.png` icon unless the node needs a purpose-specific icon.
5. Register the factory in `plugin.xml` under the appropriate IdeaFlow category.
6. Validate input schemas during configure and execution, with actionable error messages.
7. Preserve IdeaFlow metadata and internal columns that the node does not own.
8. Add unit or execution tests and extend `RegisteredNodeFactoryTest` coverage.
9. Update the node reference and changelog.

Do not add compatibility aliases for unreleased node names, settings, or ports. Until a stable public release creates a compatibility contract, the codebase should describe only its current design.

## KNIME implementation rules used by the project

- Keep algorithmic logic in testable `core` classes when it is not inherently tied to KNIME.
- Keep table conversion and metadata handling in shared `knime` support.
- Make configure-time output schemas match execution-time schemas.
- Use stable, descriptive settings keys once a version has been released.
- Save and restore every user-visible setting.
- Use explicit validation errors instead of allowing type casts or missing-column failures later.
- Keep node descriptions, port names, and dialog terminology consistent.
- Preserve deterministic behavior for a fixed seed.

## Tests

Run the complete reactor from the repository root:

```bash
mvn --batch-mode --no-transfer-progress -f ideaflow-knime/pom.xml clean verify
```

Tests should cover nominal behavior, invalid inputs, boundary values, deterministic execution, schema preservation, settings persistence, and integration between adjacent nodes. See [Testing](TESTING.md) for the current suite.

## Release build

Jenkins is the authoritative build and release pipeline. It runs the full Tycho reactor, publishes JUnit results, assembles the p2 update site, and archives release ZIPs and SHA-256 checksums. See [Jenkins](JENKINS.md) before changing versions or creating a release tag.

## Contribution checklist

Before completing a change:

- run `clean verify`;
- open the extension in the supported KNIME version when UI or port behavior changed;
- confirm affected workflows can be saved, reopened, and executed;
- update tests, node help, public documentation, and the changelog;
- check that no generated build output, credentials, or personal paths are committed.

## Reference workflows

Named algorithms are represented by KNIME workflow compositions, not a Java `recipes` package. When adding an example, use a deterministic seed, avoid machine-specific resources, state the algorithm configuration clearly, and verify the exported `.knwf` in the supported KNIME version.
