package wooga.gradle.upm.artifactory.tools

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.publish.PublishingExtension
import wooga.gradle.upm.artifactory.UPMArtifactoryExtension
import wooga.gradle.upm.artifactory.UPMProjectDeclaration

class UPMFixtures {

    GradleTestUtils utils

    UPMFixtures(GradleTestUtils utils) {
        this.utils = utils
    }

    def configureUpmProjects(String repoName, Action<? super NamedDomainObjectContainer<UPMProjectDeclaration>> cfgAction) {
        utils.requireExtension(UPMArtifactoryExtension).with { extension ->
            extension.repository = repoName
            extension.projects(cfgAction)
        }
    }

    def configurePublish(String url_ = "https://artifactoryhost/artifactory/repository", String repoName) {
        utils.requireExtension(PublishingExtension).with {
            repositories {
                upm {
                    url = url_
                    name = repoName
                }
            }
        }
    }
}
