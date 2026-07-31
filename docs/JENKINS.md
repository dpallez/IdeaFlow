# Jenkins Pipeline

[Main README](../README.md) · [Documentation index](README.md) · [Developer guide](DEVELOPMENT.md)

Jenkins is the authoritative build and release pipeline for IdeaFlow. The repository-level
[`Jenkinsfile`](../Jenkinsfile) builds the complete Tycho reactor, runs the OSGi/JUnit tests,
archives the KNIME update site, and prepares checksummed release artifacts for version tags.

## Jenkins requirements

Use a current Jenkins LTS installation with these plugins:

- Pipeline;
- Git;
- JUnit;
- Credentials Binding when deployment credentials are added later.

Configure the following tools under **Manage Jenkins → Tools**:

| Tool type | Jenkins name | Required version |
| --- | --- | --- |
| JDK | `jdk21` | Java 21 |
| Maven | `maven3` | Maven 3.9 or newer |

The build agent needs at least 2 GB of memory available to Maven and outbound HTTPS access to
the KNIME 5.11 update site and Maven Central. Linux and Windows agents are both supported.

## Creating the Jenkins job

Use a **Multibranch Pipeline** so Jenkins can distinguish ordinary branch builds from release
tags.

1. Point the branch source at the IdeaFlow Git repository.
2. Enable discovery of branches, pull requests when applicable, and tags.
3. Keep `Jenkinsfile` as the script path.
4. Configure the repository webhook to trigger a multibranch scan or build.
5. Run the `main` branch once and confirm that the test report and update-site artifact appear.

No credentials are required for normal builds. Do not place repository or deployment secrets in
the `Jenkinsfile`; store them in the Jenkins credentials store when publication is introduced.

## Ordinary builds

Every branch and pull-request build performs the following operations:

1. checks out the exact revision selected by Jenkins;
2. verifies the configured Java and Maven installations;
3. runs `mvn --batch-mode --no-transfer-progress clean verify` for the complete reactor;
4. publishes the Tycho Surefire XML reports;
5. fingerprints and archives the generated p2 update-site ZIP.

The local equivalent is:

```text
cd ideaflow-knime
mvn --batch-mode --no-transfer-progress clean verify
```

## Release builds

A release build is created from a stable tag such as `v0.1.0` or a prerelease tag such as
`v0.1.0-alpha.1`, `v0.1.0-beta.1`, or `v0.1.0-rc.1`. OSGi uses a four-part version, so Jenkins
normalizes `v0.1.0-alpha.1` to `0.1.0.alpha1`. A stable `v0.1.0` maps to `0.1.0.release`, which keeps p2 ordering correct: `alpha1 < beta1 < rc1 < release`.

For a release tag, Jenkins additionally:

1. rejects a `-SNAPSHOT` project version;
2. normalizes an optional prerelease qualifier and rejects a tag that does not match the Maven version;
3. copies the generated update site to `dist/IdeaFlow-TAG-update-site.zip`;
4. generates a SHA-256 checksum;
5. fingerprints and archives both release files.

The archived Jenkins artifacts are the authoritative release output. Publishing those artifacts
to a public update-site host can be added later as a deployment stage once the destination and
credentials have been selected.

## Test reports and retention

JUnit reports are read from:

```text
ideaflow-knime/IDEAFlow.tests/target/surefire-reports/*.xml
```

Jenkins retains 30 build records and the artifacts from the latest 10 builds. Concurrent builds
of the same job are disabled, and a complete run is limited to 45 minutes.

The planned headless KNIME workflow suite should be added as another stage in this pipeline. It
should not be implemented as a separate release workflow.

## Common failures

- **Tool installation not found:** verify that the Jenkins tool names are exactly `jdk21` and
  `maven3`.
- **Target platform cannot be resolved:** verify outbound HTTPS access to
  `https://update.knime.com/analytics-platform/5.11`.
- **No JUnit results:** inspect `IDEAFlow.tests/target/work/data/.metadata/.log` and the console
  output from the Tycho test runtime.
- **Release tag rejected:** remove the snapshot suffix, update all matching Maven/OSGi versions,
  and create a stable or prerelease tag matching the finalized project version.
