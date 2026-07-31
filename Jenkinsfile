def runMaven(final String arguments) {
    if (isUnix()) {
        sh "mvn --batch-mode --no-transfer-progress ${arguments}"
    } else {
        bat "@mvn.cmd --batch-mode --no-transfer-progress ${arguments}"
    }
}

def projectVersion() {
    final String command =
        "-f ideaflow-knime/pom.xml help:evaluate -Dexpression=project.version -DforceStdout -q"
    final String output

    if (isUnix()) {
        output = sh(script: "mvn --batch-mode --no-transfer-progress ${command}", returnStdout: true)
    } else {
        output = bat(script: "@mvn.cmd --batch-mode --no-transfer-progress ${command}", returnStdout: true)
    }

    final List<String> meaningfulLines = output.readLines()
        .collect { it.trim() }
        .findAll { it && !it.startsWith("[") }

    if (meaningfulLines.isEmpty()) {
        error("Maven did not return the project version")
    }
    return meaningfulLines.last()
}

pipeline {
    agent any

    tools {
        jdk "jdk21"
        maven "maven3"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: "30", artifactNumToKeepStr: "10"))
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 45, unit: "MINUTES")
    }

    environment {
        MAVEN_OPTS = "-Dfile.encoding=UTF-8 -Xmx2g"
    }

    stages {
        stage("Checkout") {
            steps {
                checkout scm
                script {
                    currentBuild.description = env.TAG_NAME ?: env.BRANCH_NAME ?: env.GIT_COMMIT?.take(12)
                }
            }
        }

        stage("Verify environment") {
            steps {
                script {
                    if (isUnix()) {
                        sh "java -version && mvn -version"
                    } else {
                        bat "@java -version && mvn.cmd -version"
                    }
                }
            }
        }

        stage("Build and test") {
            steps {
                runMaven("-f ideaflow-knime/pom.xml clean verify")
            }
        }

        stage("Validate release tag") {
            when {
                buildingTag()
            }
            steps {
                script {
                    if (!(env.TAG_NAME ==~ /^v[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$/)) {
                        error("Release tags must use vMAJOR.MINOR.PATCH or vMAJOR.MINOR.PATCH-PRERELEASE.NUMBER")
                    }

                    final String version = projectVersion()
                    final String tagVersion = env.TAG_NAME.substring(1)
                    final String prereleaseVersion = tagVersion.replaceFirst(
                        /-(alpha|beta|rc)\.([0-9]+)$/, '.$1$2'
                    )
                    final String expectedProjectVersion = prereleaseVersion == tagVersion
                        ? "${tagVersion}.release"
                        : prereleaseVersion

                    if (version.endsWith("-SNAPSHOT")) {
                        error("The project version is still ${version}; finalize it before tagging a release")
                    }
                    if (version != expectedProjectVersion) {
                        error("Tag ${env.TAG_NAME} expects project version ${expectedProjectVersion}, but found ${version}")
                    }
                }
            }
        }

        stage("Package release") {
            when {
                buildingTag()
            }
            steps {
                script {
                    if (isUnix()) {
                        sh "bash jenkins/package-release.sh '${env.TAG_NAME}'"
                    } else {
                        bat "@powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File jenkins\\package-release.ps1 -Tag ${env.TAG_NAME}"
                    }
                }
            }
        }
    }

    post {
        always {
            junit(
                testResults: "ideaflow-knime/IDEAFlow.tests/target/surefire-reports/*.xml",
                allowEmptyResults: false,
                keepLongStdio: true
            )
        }
        success {
            archiveArtifacts(
                artifacts: "ideaflow-knime/IDEAFlow.update/target/org.ideaflow.update-*.zip,dist/*",
                fingerprint: true
            )
        }
    }
}
