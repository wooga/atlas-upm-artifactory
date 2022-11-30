package wooga.gradle.upm.artifactory

import org.gradle.api.file.Directory
import org.gradle.api.publish.plugins.PublishingPlugin
import spock.lang.Unroll
import wooga.gradle.upm.artifactory.internal.UPMTestTools

import static wooga.gradle.upm.artifactory.internal.BasicSnippets.wrap

class UPMArtifactoryPluginIntegrationSpec extends UPMIntegrationSpec {

    @Unroll("#shouldGenerateMsg metafiles from extension property being #generateMetafiles on a project #hasMetafilesMsg metafiles")
    def "{shouldGenerateMsg} metafiles from extension property being {generateMetafiles} on a project {hasMetafilesMsg} metafiles"() {
        given: "target repository and artifact"
        def repoName = WOOGA_ARTIFACTORY_CI_REPO
        def packageName = "upm-package-name"
        and:
        def upmPackageFolder = utils.writeTestPackage(projectDir, "Assets/$packageName", packageName, "any", projectWithMetafiles)
        and:
        buildFile << """
        ${applyPlugin(UPMArtifactoryPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrap(artifactoryURL(repoName), String)}
                    name = "integration"
                }
            }
        }
        upm {
            repository = "integration"
            projects {
                defaultProject {
                    version = "any"
                    packageDirectory = ${wrap(upmPackageFolder, File)}
                    generateMetaFiles = ${wrap(generateMetafiles, Boolean)}
                }
            }
        }
        """
        when:
        def r = runTasksSuccessfully(UPMArtifactoryPlugin.GENERATE_META_FILES_TASK_NAME)
        then:
        wereMetafilesGenerated ? r.wasExecuted(UPMArtifactoryPlugin.GENERATE_META_FILES_TASK_NAME) :
                r.wasSkipped(UPMArtifactoryPlugin.GENERATE_META_FILES_TASK_NAME)

        where:
        generateMetafiles | projectWithMetafiles | wereMetafilesGenerated
        true              | true                 | true
        true              | false                | true
        false             | true                 | false
        false             | false                | true
        shouldGenerateMsg = wereMetafilesGenerated ? "generates" : "doesn't generates"
        hasMetafilesMsg = projectWithMetafiles ? "with" : "without"
    }

    @Unroll("#shouldGenerateMsg metafiles if #needMsg upm project(s) needs metafiles")
    def "generates metafiles once if any upm project needs metafiles"() {
        given: "target repository and artifact"
        def repoName = WOOGA_ARTIFACTORY_CI_REPO
        def packageName = "upm-package-name"
        and:
        def upmPackageFolder = utils.writeTestPackage(projectDir,
                "Assets/$packageName", packageName, "any", true)
        and:
        buildFile << """
        ${applyPlugin(UPMArtifactoryPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrap(artifactoryURL(repoName), String)}
                    name = "integration"
                }
            }
        }
        upm {
            repository = "integration"
            projects {
                proj1 {
                    packageDirectory = ${wrap(upmPackageFolder, File)}
                    generateMetaFiles = ${wrap(projectGeneratesMetafiles[0], Boolean)}
                }
                proj2 {
                    packageDirectory = ${wrap(upmPackageFolder, File)}
                    generateMetaFiles = ${wrap(projectGeneratesMetafiles[1], Boolean)}
                }
                proj3 {
                    packageDirectory = ${wrap(upmPackageFolder, File)}
                    generateMetaFiles = ${wrap(projectGeneratesMetafiles[2], Boolean)}
                }
                proj4 {
                    packageDirectory = ${wrap(upmPackageFolder, File)}
                    generateMetaFiles = ${wrap(projectGeneratesMetafiles[3], Boolean)}
                }
            }
        }
        """
        when:
        def r = runTasksSuccessfully(UPMArtifactoryPlugin.GENERATE_META_FILES_TASK_NAME)
        then:
        wereMetafilesGenerated ? r.wasExecuted(UPMArtifactoryPlugin.GENERATE_META_FILES_TASK_NAME) :
                r.wasSkipped(UPMArtifactoryPlugin.GENERATE_META_FILES_TASK_NAME)

        where:
        projectGeneratesMetafiles    | wereMetafilesGenerated
        [true, false, false, false]  | true
        [true, true, true, false]    | true
        [false, false, false, false] | false
        shouldGenerateMsg = wereMetafilesGenerated ? "generates" : "doesn't generates"
        needMsg = wereMetafilesGenerated ? "${projectGeneratesMetafiles.count { it }}" : "no"
    }

    def "publishes upm project as UPM package on gradle subproject"() {
        given: "a gradle subproject"
        def subprojDir = new File(projectDir, "subproject")
        def subBuildFile = initializeSubproject(subprojDir)
        and: "root project with publishing plugin"
        buildFile << applyPlugin(PublishingPlugin)

        and: "existing UPM-ready folder in subproject"
        utils.writeTestPackage(subprojDir, upmPackageFolder, packageName, packageVersion)
        and: "artifactory credentials"
        def (username, password) = utils.credentialsFromEnv()
        and: "configured package dir and repository"
        subBuildFile << """
        ${applyPlugin(UPMArtifactoryPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrap(artifactoryURL(WOOGA_ARTIFACTORY_CI_REPO), String)}
                    name = ${wrap(repositoryName, String)}
                }
            }
        }
        upm {
            repository = ${wrap(repositoryName, String)}
            username = ${wrap(username, String)}
            password = ${wrap(password, String)}
            projects {
                defaultProject {
                    packageDirectory = ${wrap(upmPackageFolder, Directory)}
                    version = ${wrap(packageVersion, String)}
                }
            }
        }
        """

        when:
        def r = runTasks("publish")

        then:
        r.success
        utils.hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, packageName, packageVersion)

        where:
        upmPackageFolder     | repositoryName
        "Assets/upm-package" | "integration"
        packageVersion = "0.0.1"
        packageName = "upm-package-name"
    }

    def "publishes project as UPM package"() {
        given: "existing UPM-ready folder"
        utils.writeTestPackage(projectDir, upmPackageFolder, packageName, packageVersion)
        and: "artifactory credentials"
        def (username, password) = utils.credentialsFromEnv()
        and: "configured package dir and repository"
        buildFile << """
        ${applyPlugin(UPMArtifactoryPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrap(artifactoryURL(WOOGA_ARTIFACTORY_CI_REPO), String)}
                    name = ${wrap(repositoryName, String)}
                }
            }
        }
        upm {
            repository = ${wrap(repositoryName, String)}
            username = ${wrap(username, String)}
            password = ${wrap(password, String)}
            projects {
                defaultProject {
                    packageDirectory = ${wrap(upmPackageFolder, Directory)}
                    version = ${wrap(packageVersion, String)}
                }
            }
        }
        """

        when:
        def r = runTasksSuccessfully("publish")

        then:
        utils.hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, packageName, packageVersion)

        where:
        upmPackageFolder     | repositoryName
        "Assets/upm-package" | "integration"
        packageVersion = "0.0.1"
        packageName = "upm-package-name"
    }

    def "publishes many projects as UPM packages"() {
        given: "existing UPM-ready folders"
        [upmPackageFolders, packageNames].transpose().each { tuple ->
            def (String folder, String pkgName) = tuple
            utils.writeTestPackage(projectDir, folder, pkgName)
        }
        and: "artifactory credentials"
        def (username, password) = utils.credentialsFromEnv()
        and: "configured package dir and repository"
        buildFile << """
        ${applyPlugin(UPMArtifactoryPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrap(artifactoryURL(WOOGA_ARTIFACTORY_CI_REPO), String)}
                    name = ${wrap(repositoryName, String)}
                }
            }
        }
        upm {
            repository = ${wrap(repositoryName, String)}
            username = ${wrap(username, String)}
            password = ${wrap(password, String)}
            projects {
                ${(0..packageNames.size() - 1).collect { i ->
            """${packageNames[i]} {
                    packageDirectory = ${wrap(upmPackageFolders[i], Directory)}
                    version = ${wrap(versions[i], String)}
                }"""
        }.join("\n")}                
            }
        }
        """

        when:
        def r = runTasksSuccessfully("publish")

        then:
        [packageNames, versions].transpose().every { def tuple ->
            def (String packageName, String version) = tuple
            utils.hasPackageOnArtifactory(UPMTestTools.WOOGA_ARTIFACTORY_CI_REPO, packageName, version)
        }

        where:
        packageNames                         | versions
        ["package1", "package2", "package3"] | ["0.0.1", "0.0.2", "0.0.3"]
        upmPackageFolders = packageNames.collect { "Assets/upm-$it".toString() }
        repositoryName = "integration"
    }

    @Unroll("publishes project as UPM package to protected repository with credentials set in #location")
    def "publishes project as UPM package to protected repository with credentials set in {location}"() {
        given: "existing UPM-ready folder"
        utils.writeTestPackage(projectDir, packageDirectory, packageName)
        and: "artifactory credentials"
        def (username, password) = utils.credentialsFromEnv()
        and: "configured package dir and repository"
        buildFile << """
        ${applyPlugin(UPMArtifactoryPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrap(artifactoryURL(WOOGA_ARTIFACTORY_CI_REPO), String)}
                    name = "integration"
                    ${location == "repository" ? """
                        credentials {
                            username = ${wrap(username, String)}
                            password = ${wrap(password, String)}
                        }
                        """ : ""}
                }
            }
        }
        upm {
            repository = "integration"
            ${location == "extension" ? """ 
            username = ${wrap(username, String)}
            password = ${wrap(password, String)}""" : ""}
            projects {
                defaultProject { 
                    packageDirectory = ${wrap(packageDirectory, Directory)}
                    version = ${wrap(DEFAULT_VERSION, String)}
                }
            }
        }
        """

        when:
        def r = runTasks("publish")

        then:
        r.success
        utils.hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, packageName)

        where:
        location << ["extension", "repository"]
        packageDirectory = "Assets/upm"
        packageName = "packageName"
    }
}
