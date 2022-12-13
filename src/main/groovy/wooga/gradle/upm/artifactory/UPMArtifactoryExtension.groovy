package wooga.gradle.upm.artifactory

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import wooga.gradle.upm.artifactory.internal.Extensions
import wooga.gradle.upm.artifactory.internal.repository.UPMArtifactRepository
import wooga.gradle.upm.artifactory.traits.UPMPublishSpec

class UPMArtifactoryExtension implements UPMPublishSpec {


    final NamedDomainObjectContainer<UPMProjectDeclaration> projects
    protected Provider<UPMArtifactRepository> selectedUPMRepository

    static UPMArtifactoryExtension withProjectsContainer(Project project, String extensionName) {
        NamedDomainObjectFactory<UPMProjectDeclaration> projectsFactory = {
            String name -> UPMProjectDeclaration.withStaticConventions(project, name)
        }
        def projects = project.objects.domainObjectContainer(UPMProjectDeclaration, projectsFactory)

        return project.extensions.create(extensionName, UPMArtifactoryExtension, projects)
    }

    static UPMArtifactoryExtension withStaticConventions(Project project, String name) {
        def extension = withProjectsContainer(project, name)
        Extensions.setPropertiesOwner(UPMArtifactoryExtension, extension, name)
        extension.with {
            repository.convention(UPMArtifactoryConventions.repository.getStringValueProvider(project))
            username.convention(UPMArtifactoryConventions.username.getStringValueProvider(project))
            password.convention(UPMArtifactoryConventions.password.getStringValueProvider(project))
        }
        return extension
    }

    static UPMArtifactoryExtension withPublishingConventions(Project project, PublishingExtension publishingExt, String name) {
        def extension = withStaticConventions(project, name)
        extension.with {
            it.repositories.set(project.provider({
                def repos = upmReposFromPublishing(publishingExt)
                return repos.size() > 0 ? repos : null
            }))

            it.selectedUPMRepository = it.repositories.flatMap {
                upmRepos -> extension.repository.map{ upmRepos[it]}
            }
            username.convention(UPMArtifactoryConventions.username.getStringValueProvider(project)
                    .orElse(selectedUPMRepository.map {it.credentials?.username}))
            password.convention(UPMArtifactoryConventions.password.getStringValueProvider(project)
                    .orElse(selectedUPMRepository.map {it.credentials?.password}))
        }
        return extension
    }


    UPMArtifactoryExtension(NamedDomainObjectContainer<UPMProjectDeclaration> projects) {
        this.projects = projects
    }

    Provider<List<UPMProjectDeclaration>> getProjectsProvider() {
        def projectList = objects.listProperty(UPMProjectDeclaration)

        projects.configureEach { UPMProjectDeclaration upmProject ->
            projectList.add(upmProject)
        }
        return projectList
    }

    public void projects(Action<? super NamedDomainObjectContainer<UPMProjectDeclaration>> action) {
        action.execute(projects);
    }

    private static Map<String, UPMArtifactRepository> upmReposFromPublishing(PublishingExtension publishExt) {
        return publishExt.repositories.withType(UPMArtifactRepository).collectEntries {
            return [(it.name): it]
        }
    }

    Provider<String> getUpmRepositoryBaseUrl() {
        return selectedUPMRepository.map {it.baseUrl}
    }

    Provider<String> getUpmRepositoryKey() {
        return selectedUPMRepository.map{it.repositoryKey}
    }
}
