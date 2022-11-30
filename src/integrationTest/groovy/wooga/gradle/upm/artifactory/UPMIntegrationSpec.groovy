package wooga.gradle.upm.artifactory

import com.wooga.gradle.test.IntegrationSpec
import com.wooga.gradle.test.executable.FakeExecutables
import spock.lang.Shared
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.upm.artifactory.internal.BasicSnippetsTrait
import wooga.gradle.upm.artifactory.internal.GradleTestTools
import wooga.gradle.upm.artifactory.internal.UPMTestTools

class UPMIntegrationSpec extends IntegrationSpec implements BasicSnippetsTrait {

    static final String DEFAULT_VERSION = UPMTestTools.DEFAULT_VERSION
    static final String WOOGA_ARTIFACTORY_CI_REPO = UPMTestTools.WOOGA_ARTIFACTORY_CI_REPO

    @Shared
    long specStartupTime
    @Shared
    UPMTestTools utils
    @Shared
    protected File unityTestLocation

    def setupSpec() {
        this.specStartupTime = System.currentTimeMillis()
        this.utils = new UPMTestTools()
    }


    def setup() {
        def unityFakeExecutable = FakeExecutables.argsReflector(new File(projectDir, "fakeUnity").absolutePath, 0)
        unityTestLocation = unityFakeExecutable.executable
        buildFile << """
            ${applyPlugin(UnityPlugin)}
            unity.unityPath.set(${wrap(unityTestLocation, File)})
        """.stripIndent()
    }

    def initializeSubproject(File settingsFile = this.settingsFile, File subprojectDir) {
        GradleTestTools.initializeSubproject(settingsFile, subprojectDir)
    }


    def cleanup() {
        utils.cleanupArtifactoryRepoInRange(specStartupTime, System.currentTimeMillis())
    }

    String artifactoryURL(String repoName) {
        return utils.artifactoryURL(repoName)
    }
}
