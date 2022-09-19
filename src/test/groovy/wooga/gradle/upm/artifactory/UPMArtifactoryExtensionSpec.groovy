package wooga.gradle.upm.artifactory

import nebula.test.ProjectSpec
import org.gradle.api.InvalidUserDataException
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.internal.provider.MissingValueException
import spock.lang.Unroll
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
}
