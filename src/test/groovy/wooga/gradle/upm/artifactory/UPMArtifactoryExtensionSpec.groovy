package wooga.gradle.upm.artifactory

import nebula.test.ProjectSpec
import org.gradle.api.InvalidUserDataException
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.internal.provider.ValueSupplier
import org.gradle.api.provider.Provider
import spock.lang.Unroll
import wooga.gradle.upm.artifactory.internal.repository.UPMArtifactRepository
import wooga.gradle.upm.artifactory.internal.repository.UPMRepository
import wooga.gradle.upm.artifactory.tools.UPMFixtures
import wooga.gradle.upm.artifactory.tools.GradleTestUtils

class UPMArtifactoryExtensionSpec extends ProjectSpec {

    private GradleTestUtils utils
    private UPMFixtures fixtures

    def setup() {
        this.utils = new GradleTestUtils(project)
        this.fixtures = new UPMFixtures(utils)
    }

    def "sets extension properties dependent on the publishing extension"() {
        given:
        project.plugins.apply(UPMArtifactoryPlugin)
        and:
        project.extensions.getByType(UPMArtifactoryExtension).with {
            it.repository = "test"
        }
        and:
        fixtures.configurePublish("$baseURL/$repositoryKey", "test")

        when:
        utils.evaluate(project)

        then:
        def ext = project.extensions.getByType(UPMArtifactoryExtension)
        ext.upmRepositoryBaseUrl.present
        ext.upmRepositoryBaseUrl.get() == baseURL
        ext.upmRepositoryKey.present
        ext.upmRepositoryKey.get() == repositoryKey

        where:
        baseURL = "https://jfrogrepo/artifactory"
        repositoryKey = "repository"
    }

    def "throws on project evaluation if repository property is not set"() {
        given:
        project.plugins.apply(UPMArtifactoryPlugin)
        and:
        fixtures.configurePublish("any")

        when:
        utils.evaluate(project)

        then:
        def e = thrown(ProjectConfigurationException)
        e.cause instanceof MissingValueException
    }

    def "throws on project evaluation if selected repository does not exists"() {
        given:
        project.plugins.apply(UPMArtifactoryPlugin)
        and:
        fixtures.configurePublish("repo")
        def upmExt = utils.requireExtension(UPMArtifactoryExtension)
        upmExt.repository = "otherRepo"

        when:
        utils.evaluate(project)

        then:
        def e = thrown(ProjectConfigurationException)
        e.cause instanceof MissingValueException
    }


    @Unroll("throws on project evaluation if selected repository url is not valid #invalidURL")
    def "throws on project evaluation if selected repository url is not valid"() {
        given:
        project.plugins.apply(UPMArtifactoryPlugin)
        and:
        fixtures.configurePublish(invalidURL, "test")
        def upmExt = utils.requireExtension(UPMArtifactoryExtension)
        upmExt.repository = "test"

        when:
        utils.evaluate(project)

        then:
        def e = thrown(ProjectConfigurationException)
        e.cause instanceof InvalidUserDataException

        where:
        invalidURL << ["whithoutprotocol/artifactory/repository", "https://withoutrepopath"]
    }

    def "throws on project evaluation if no publish repository is set"() {
        given:
        project.plugins.apply(UPMArtifactoryPlugin)
        and:
        def upmExt = project.extensions.getByType(UPMArtifactoryExtension)
        upmExt.repository = "repo"

        when:
        utils.evaluate(project)

        then:
        def e = thrown(ProjectConfigurationException)
        e.cause instanceof MissingValueException
    }

    @Unroll
    def "correct repository is set up"() {
        given:
        project.plugins.apply(UPMArtifactoryPlugin)
        and:
        def upmExt = utils.requireExtension(UPMArtifactoryExtension).with {
            repository = selectedRepository
            if (repositories != null) {
                it.repositories = repositories.collectEntries { name, url ->
                    return [(name): new UPMRepository().with { it.name(name); it.url(url); return it; }]
                }
            }
            return it
        }

        when:
        def entries = convention?.collectEntries { name, url ->
            return [(name): new UPMRepository().with { it.name(name); it.url(url); return it; }]
        }
        upmExt.repositories.convention(project.provider{entries as Map<String, UPMRepository>})

        then:
        upmExt.repositories.orNull?.size() == repositories?.size()?: convention.size()
        upmExt.upmRepositoryBaseUrl.isPresent()
        upmExt.upmRepositoryKey.isPresent()

        where:
        repositories                                                   | convention                                                     | selectedRepository
        ["repository": "https://myurl.com/repositories/repo"]          | null                                                           | "repository"
        ["repo": "https://m.com/rs/r"]                                 | ["repo2": "https://m.com/rs/r2", "r3": "https://m.com/rs/r3"]  | "repo"
        ["repo": "https://m.com/rs/r", "repo2": "https://m.com/rs/r2"] | null                                                           | "repo"
        null                                                           | ["repo": "https://m.com/rs/r", "repo2": "https://m.com/rs/r2"] | "repo2"

    }
}
