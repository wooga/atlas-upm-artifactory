package wooga.gradle.upm.artifactory

import com.wooga.gradle.test.PropertyLocation
import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import com.wooga.gradle.test.writers.PropertySetterWriter
import org.gradle.api.file.Directory
import spock.lang.Unroll
import wooga.gradle.upm.artifactory.internal.UPMSnippetsTrait

import static wooga.gradle.upm.artifactory.internal.BasicSnippets.wrap
import static com.wooga.gradle.test.writers.PropertySetInvocation.*

class UPMArtifactoryExtensionIntegrationSpec extends UPMIntegrationSpec implements UPMSnippetsTrait {

    static final String existingUPMPackage = "upmPackage"

    def setup() {
        buildFile << """
            ${applyPlugin(UPMArtifactoryPlugin)}
        """.stripIndent()
    }

    @Unroll("can set property upm.#property with #invocation and type #type with build.gradle")
    def "can set property on upm extension with build.gradle"() {
        given: "existing UPM-ready folder"
        utils.writeTestPackage(projectDir, existingUPMPackage, existingUPMPackage)
        and:
        buildFile << """
            ${singlePublishingUPMRepository(property == "repository" ? rawValue.toString() : "any")}
            ${property != "repository" ? "upm {repository = ${wrap("any", String)}}" : ""}
        """

        when:
        set.location = invocation == none ? PropertyLocation.none : set.location

        def propertyQuery = runPropertyQuery(get, set)
                .withSerializer(Directory, {
                    String dir -> dir ? new File(projectDir, dir).absolutePath : null
                }).withSerializer("Provider<Directory>", {
            String dir -> dir ? new File(new File(projectDir, "build"), dir).absolutePath : null
        })
        then:
        propertyQuery.matches(rawValue)

        where:
        property     | invocation  | rawValue   | type
        "repository" | providerSet | "repoName" | "Provider<String>"
        "repository" | assignment  | "repoName" | "Provider<String>"
        "repository" | setter      | "repoName" | "String"
        "repository" | assignment  | "repoName" | "String"


        "username"   | providerSet | "user"     | "Provider<String>"
        "username"   | assignment  | "user"     | "Provider<String>"
        "username"   | setter      | "user"     | "String"
        "username"   | assignment  | "user"     | "String"
        "username"   | none        | null       | "String"

        "password"   | providerSet | "pwd"      | "Provider<String>"
        "password"   | assignment  | "pwd"      | "Provider<String>"
        "password"   | setter      | "pwd"      | "String"
        "password"   | assignment  | "pwd"      | "String"
        "password"   | none        | null       | "String"

        set = new PropertySetterWriter("upm", property)
                .set(rawValue, type)
                .toScript(invocation)
        get = new PropertyGetterTaskWriter(set)

    }

    @Unroll("can set property upm.project.#property with #invocation and type #type with build.gradle")
    def "can set property on upm project with build.gradle"() {
        given: "existing UPM-ready folder"
        utils.writeTestPackage(projectDir, existingUPMPackage, existingUPMPackage)
        and:
        buildFile << """
            ${singlePublishingUPMRepository("any")}
            upm {
                repository = ${wrap("any", String)}
                projects {
                    $projectName {}
                }
            }
            def upmProject = upm.projects.getByName(\"$projectName\")
        """

        when:
        set.location = invocation == none ? PropertyLocation.none : set.location

        def propertyQuery = runPropertyQuery(get, set)
                .withSerializer(Directory, {
                    String dir -> dir ? new File(projectDir, dir).absolutePath : null
                }).withSerializer("Provider<Directory>", {
            String dir -> dir ? new File(new File(projectDir, "build"), dir).absolutePath : null
        })

        then:
        propertyQuery.matches(rawValue)

        //todo add extra somehting to test dynamic properties
        where:
        property            | invocation  | rawValue      | type
        "version"           | providerSet | "0.0.1"       | "Provider<String>"
        "version"           | assignment  | "0.0.1"       | "Provider<String>"
        "version"           | setter      | "0.0.1"       | "String"
        "version"           | assignment  | "0.0.1"       | "String"
        "version"           | none        | "unspecified" | "String" //unspecified is the default for project.version

        "packageDirectory"  | providerSet | "upmpkg"      | "Provider<Directory>"
        "packageDirectory"  | assignment  | "upmpkg"      | "Provider<Directory>"
        "packageDirectory"  | setter      | "upmpkg"      | "Directory"
        "packageDirectory"  | assignment  | "upmpkg"      | "Directory"
        "packageDirectory"  | none        | null          | "Directory"

        "generateMetaFiles" | providerSet | true          | "Provider<Boolean>"
        "generateMetaFiles" | assignment  | false         | "Provider<Boolean>"
        "generateMetaFiles" | setter      | true          | "Boolean"
        "generateMetaFiles" | assignment  | false         | "Boolean"
        "generateMetaFiles" | none        | false         | "Boolean"

        projectName = "sample"
        set = new PropertySetterWriter("upmProject", property)
                .set(rawValue, type)
                .toScript(invocation)
        get = new PropertyGetterTaskWriter(set)
    }

    @Unroll("can set property upm.#property with env var #envVar")
    def "can set extension property from environment"() {
        given: "minimal configured upm project"
        buildFile << minimalUPMConfiguration(projectDir, existingUPMPackage, property == "repository" ? rawValue : "any")
        buildFile << """ upm {
            ${property == "repository" ? "repository = null" : ""}
        }
        """

        when:
        def propertyQuery = runPropertyQuery(get, set).withSerializer(Directory) {
            String dir -> new File(projectDir, dir).absolutePath
        }

        then:
        propertyQuery.matches(rawValue)

        where:
        property     | envVar           | type   | rawValue
        "repository" | "UPM_REPOSITORY" | String | "repoName"
        "username"   | "UPM_USR"        | String | "username"
        "username"   | "UPM_USERNAME"   | String | "username"
        "password"   | "UPM_PWD"        | String | "passw0rd"
        "password"   | "UPM_PASSWORD"   | String | "passw0rd"

        set = new PropertySetterWriter("upm", property)
                .set(rawValue, type)
                .withEnvironmentKey(envVar)
                .to(PropertyLocation.environment)

        get = new PropertyGetterTaskWriter(set)
    }

    @Unroll("can set upm project #projectName property upm.project.#property with env var #envVar")
    def "can set upm project property from environment"() {
        given: "minimal configured upm project"
        buildFile << minimalUPMConfiguration(projectDir, existingUPMPackage, property == "repository" ? rawValue : "any")
        buildFile << """ upm {
            projects {
                $projectName {
                    ${property == "packageDirectory" ? "packageDirectory = null" : ""}
                }
            }
        }
        def upmProject = upm.projects.getByName(\"$projectName\")
        """

        when:
        def propertyQuery = runPropertyQuery(get, set).withSerializer(Directory) {
            String dir -> new File(projectDir, dir).absolutePath
        }

        then:
        propertyQuery.matches(rawValue)

        where:
        projectName | property            | envVar                           | type      | rawValue
        "sample"    | "version"           | "UPM_SAMPLE_VERSION"             | String    | "0.0.1"
        "sample"    | "version"           | "UPM_SAMPLE_PACKAGE_VERSION"     | String    | "0.0.1"
        "sample"    | "packageDirectory"  | "UPM_SAMPLE_PACKAGE_DIR"         | Directory | "wdk-name"
        "sample"    | "packageDirectory"  | "UPM_SAMPLE_PACKAGE_DIRECTORY"   | Directory | "wdk-name"
        "sample"    | "generateMetaFiles" | "UPM_SAMPLE_GENERATE_METAFILES"  | Boolean   | "true"
        "sample"    | "generateMetaFiles" | "UPM_SAMPLE_GENERATE_META_FILES" | Boolean   | "false"
        "sample"    | "generateMetaFiles" | "UPM_SAMPLE_GENERATE_META_FILES" | Boolean   | "false"

        "sample"    | "version"           | "UPM_VERSION"                    | String    | "0.0.1"
        "sample"    | "version"           | "UPM_PACKAGE_VERSION"            | String    | "0.0.1"
        "sample"    | "packageDirectory"  | "UPM_PACKAGE_DIR"                | Directory | "wdk-name"
        "sample"    | "packageDirectory"  | "UPM_PACKAGE_DIRECTORY"          | Directory | "wdk-name"
        "sample"    | "generateMetaFiles" | "UPM_GENERATE_METAFILES"         | Boolean   | "true"
        "sample"    | "generateMetaFiles" | "UPM_GENERATE_META_FILES"        | Boolean   | "false"

        set = new PropertySetterWriter("upmProject", property)
                .set(rawValue, type)
                .withEnvironmentKey(envVar)
                .to(PropertyLocation.environment)

        get = new PropertyGetterTaskWriter(set)
    }

    @Unroll("prioritize project-targeted env var #namedEnvVar over #unamedEnvVars when setting upm.project.#property")
    def "prioritizes environments with project name when setting upm project properties"() {
        given: "minimal configured upm project"
        buildFile << minimalUPMConfiguration(projectDir, existingUPMPackage, property == "repository" ? rawValue : "any")
        buildFile << """ upm {
            projects {
                $projectName {
                    ${property == "packageDirectory" ? "packageDirectory = null" : ""}
                }
            }
        }
        def upmProject = upm.projects.getByName(\"$projectName\")
        """
        environmentVariables.set(namedEnvVar, expected)
        unamedEnvVars.each { environmentVariables.set(it, notExpected) }
        when:
        def propertyQuery = runPropertyQuery(get).withSerializer(Directory) {
            String dir -> new File(projectDir, dir).absolutePath
        }

        then:
        propertyQuery.matches(expected)

        where:
        property            | namedEnvVar                      | unamedEnvVars                                         | type
        "version"           | "UPM_SAMPLE_VERSION"             | ["UPM_VERSION", "UPM_PACKAGE_VERSION"]                | String
        "version"           | "UPM_SAMPLE_PACKAGE_VERSION"     | ["UPM_VERSION", "UPM_PACKAGE_VERSION"]                | String
        "packageDirectory"  | "UPM_SAMPLE_PACKAGE_DIR"         | ["UPM_PACKAGE_DIR", "UPM_PACKAGE_DIRECTORY"]          | Directory
        "packageDirectory"  | "UPM_SAMPLE_PACKAGE_DIRECTORY"   | ["UPM_PACKAGE_DIR", "UPM_PACKAGE_DIRECTORY"]          | Directory
        "generateMetaFiles" | "UPM_SAMPLE_GENERATE_METAFILES"  | ["UPM_GENERATE_METAFILES", "UPM_GENERATE_META_FILES"] | Boolean
        "generateMetaFiles" | "UPM_SAMPLE_GENERATE_META_FILES" | ["UPM_GENERATE_METAFILES", "UPM_GENERATE_META_FILES"] | Boolean
        projectName = "sample"
        expected = type == Boolean ? "true" : "value"
        notExpected = type == Boolean ? "nottrue" : "otherValue"

        get = new PropertyGetterTaskWriter("upmProject.${property}").with {
            it.typeName = type.simpleName
            return it
        }

    }


    @Unroll("can set property upm.#property with gradle property #gradlePropName")
    def "can set extension property with gradle property"() {
        given: "minimal configured upm project"
        buildFile << minimalUPMConfiguration(projectDir, existingUPMPackage, property == "repository" ? rawValue : "any")
        buildFile << """ upm {
            ${property == "repository" ? "repository = null" : ""}
        }"""

        when:
        def propertyQuery = runPropertyQuery(get, set).withSerializer(Directory) {
            String dir -> new File(projectDir, dir).absolutePath
        }

        then:
        propertyQuery.matches(rawValue)

        where:
        property     | gradlePropName       | type   | rawValue
        "repository" | "upm.repository"     | String | "repoName"
        "repository" | "publish.repository" | String | "repoName"
        "username"   | "upm.username"       | String | "username"
        "username"   | "publish.username"   | String | "username"
        "password"   | "upm.password"       | String | "passw0rd"
        "password"   | "publish.password"   | String | "passw0rd"

        set = new PropertySetterWriter("upm", property)
                .set(rawValue, type)
                .withPropertyKey(gradlePropName)
                .to(PropertyLocation.propertyCommandLine)
        get = new PropertyGetterTaskWriter(set)
    }

    @Unroll("can set property upm.project.#property with gradle property #gradlePropName")
    def "can set upm project property with gradle property"() {
        given: "minimal configured upm project"
        buildFile << minimalUPMConfiguration(projectDir, existingUPMPackage, property == "repository" ? rawValue : "any")
        buildFile << """ upm {
            projects {
                $projectName {
                }
                
            }
        }
        def upmProject = upm.projects.getByName(\"$projectName\")
        """

        when:
        def propertyQuery = runPropertyQuery(get, set).withSerializer(Directory) {
            String dir -> new File(projectDir, dir).absolutePath
        }

        then:
        propertyQuery.matches(rawValue)

        where:
        projectName | property            | gradlePropName                  | type      | rawValue
        "sample"    | "version"           | "upm.sample.version"            | String    | "0.0.1"
        "sample"    | "version"           | "upm.sample.package.version"    | String    | "0.0.1"
        "sample"    | "version"           | "sample.publish.version"        | String    | "0.0.1"
        "sample"    | "packageDirectory"  | "upm.sample.package.directory"  | Directory | "wdk-name"
        "sample"    | "generateMetaFiles" | "upm.sample.generate.metafiles" | Boolean   | "true"

        "sample"    | "version"           | "upm.version"                   | String    | "0.0.1"
        "sample"    | "version"           | "upm.package.version"           | String    | "0.0.1"
        "sample"    | "version"           | "publish.version"               | String    | "0.0.1"
        "sample"    | "packageDirectory"  | "upm.package.directory"         | Directory | "wdk-name"
        "sample"    | "generateMetaFiles" | "upm.generate.metafiles"        | Boolean   | "true"

        set = new PropertySetterWriter("upmProject", property)
                .set(rawValue, type)
                .withPropertyKey(gradlePropName)
                .to(PropertyLocation.propertyCommandLine)
        get = new PropertyGetterTaskWriter(set)
    }

    @Unroll("prioritize project-targeted gradle property #namedGradleProp over #unamedGradleProps when setting upm.project.#property")
    def "prioritizes gradle properties with project name when setting upm project properties"() {
        given: "minimal configured upm project"
        buildFile << minimalUPMConfiguration(projectDir, existingUPMPackage, property == "repository" ? rawValue : "any")
        buildFile << """ upm {
            projects {
                $projectName {
                    ${property == "packageDirectory" ? "packageDirectory = null" : ""}
                }
            }
        }
        def upmProject = upm.projects.getByName(\"$projectName\")
        """

        when:
        def propertyQuery = runPropertyQuery(get, *sets).withSerializer(Directory) {
            String dir -> new File(projectDir, dir).absolutePath
        }

        then:
        propertyQuery.matches(expected)

        where:
        property            | namedGradleProp                 | unamedGradleProps                                         | type
        "version"           | "upm.sample.version"            | ["upm.version", "upm.package.version", "publish.version"] | String
        "version"           | "upm.sample.package.version"    | ["upm.version", "upm.package.version", "publish.version"] | String
        "version"           | "sample.publish.version"        | ["upm.version", "upm.package.version", "publish.version"] | String
        "packageDirectory"  | "upm.sample.package.directory"  | ["upm.package.directory"]                                 | Directory
        "generateMetaFiles" | "upm.sample.generate.metafiles" | ["upm.generate.metafiles", "UPM_GENERATE_META_FILES"]     | Boolean
        projectName = "sample"
        expected = type == Boolean ? "true" : "value"
        notExpected = type == Boolean ? "nottrue" : "otherValue"

        get = new PropertyGetterTaskWriter("upmProject.${property}").with {
            it.typeName = type.simpleName
            return it
        }
        sets = [
                new PropertySetterWriter("upmProject", property)
                        .set(expected, type)
                        .withPropertyKey(namedGradleProp)
                        .to(PropertyLocation.propertyCommandLine),
                *(unamedGradleProps.collect { unamedProp ->
                    new PropertySetterWriter("upmProject", property)
                            .set(notExpected, type)
                            .withPropertyKey(unamedProp)
                            .to(PropertyLocation.propertyCommandLine)
                })
        ]
    }

    @Unroll
    def "sets credentials configured in the publishing extension to the UPM extension"() {
        given: "existing UPM-ready folder"
        utils.writeTestPackage(projectDir, existingUPMPackage, existingUPMPackage)
        and: "configured build.gradle file"
        buildFile << """
        publishing {
            repositories {
                upm {
                    url = "https://artifactoryhost/artifactory/repository"
                    name = "any"
                    credentials {
                        ${property} = ${wrap(rawValue, type)}
                    }
                }
            }
        }
        upm {repository = "any"}
        """
        when:
        def propertyQuery = runPropertyQuery(get)

        then:
        propertyQuery.matches(rawValue)

        where:
        property   | type   | rawValue
        "username" | String | "username"
        "password" | String | "password"

        get = new PropertyGetterTaskWriter("upm.${property}")
    }

}
