package wooga.gradle.upm.artifactory

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.provider.Provider
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskProvider
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.tasks.Unity
import wooga.gradle.upm.artifactory.internal.UPMProjectConfigurator
import wooga.gradle.upm.artifactory.internal.repository.DefaultUPMRepositoryHandlerConvention

class UPMArtifactoryPlugin implements Plugin<Project> {

    static final String GROUP = "UPM"
    static final String EXTENSION_NAME = "upm"
    static final String GENERATE_META_FILES_TASK_NAME = "generateMetaFiles"
    static final String ROOT_ARCHIVE_CONFIGURATION_NAME = "upm"
    static final String GENERATE_UPM_PACKAGE_TASK_SUFFIX = UPMProjectConfigurator.GENERATE_UPM_PACKAGE_TASK_SUFFIX
    static final String ARCHIVE_CONFIGURATION_SUFFIX = UPMProjectConfigurator.ARCHIVE_CONFIGURATION_SUFFIX
    static final String PUBLICATION_SUFFIX = UPMProjectConfigurator.PUBLICATION_SUFFIX

    Project project

    @Override
    void apply(Project project) {
        project.plugins.apply(UnityPlugin.class)
        project.plugins.apply(ArtifactoryPlugin.class)
        project.plugins.apply(IvyPublishPlugin.class)
        project.plugins.apply(PublishingPlugin.class)

        this.project = project

        def publishingExt = configurePublishingExtension(project)
        def extension = UPMArtifactoryExtension.withPublishingConventions(project, publishingExt, EXTENSION_NAME)
        def rootConfiguration = project.configurations.register(ROOT_ARCHIVE_CONFIGURATION_NAME)
        def generateMetafiles = project.tasks.register(GENERATE_META_FILES_TASK_NAME, Unity) {
            it.group = GROUP
        }
        def projectsPublications = configureUPMProjects(extension, publishingExt, generateMetafiles, rootConfiguration)
        configureArtifactory(project, extension, projectsPublications)
        setupPublishTasksDependencies(project)
    }

    Provider<? extends List<Publication>> configureUPMProjects(UPMArtifactoryExtension extension,
                                                               PublishingExtension publishingExt,
                                                               TaskProvider<Unity> generateMetafiles,
                                                               NamedDomainObjectProvider<Configuration> rootConfiguration) {
        def publications = project.objects.listProperty(Publication)
        extension.projects.configureEach {
            generateMetafiles.configure {task ->
                task.onlyIf = {
                    extension.projects.collect {it.createConfigurator(project) }.any {
                        upmConfig -> upmConfig.shouldGenerateMetafiles(task)
                    }
                }
            }
            def upmConfig = it.createConfigurator(project)
            def publication = upmConfig.publicationForSingleUpmProject(publishingExt, generateMetafiles, rootConfiguration)
            publications.add(publication)
        }
        return publications
    }

    private static PublishingExtension configurePublishingExtension(Project project) {
        //Extends the publishing extension to be able to hold upm repositories
        def publishing = project.extensions.getByType(PublishingExtension)
        publishing.with {
            def upmHandlerConvention = new DefaultUPMRepositoryHandlerConvention(it.repositories as DefaultRepositoryHandler)
            project.convention.plugins.put(UPMArtifactoryPlugin.canonicalName, upmHandlerConvention)
            new DslObject(it.repositories).convention.plugins.put(UPMArtifactoryPlugin.canonicalName, upmHandlerConvention)
        }
        return publishing
    }

    private static void configureArtifactory(Project project, UPMArtifactoryExtension upmExtension, Provider<List<Publication>> publications) {

        project.afterEvaluate { -> //Needs to be done after evaluate in order to fill the extension fields
            def artifactory = project.convention.plugins.get("artifactory") as ArtifactoryPluginConvention
            artifactory.contextUrl = upmExtension.upmRepositoryBaseUrl.get()
            artifactory.publish { PublisherConfig publisherConfig ->
                publisherConfig.repository { it ->
                    it.ivy { PublisherConfig.Repository repo ->
                        repo.artifactLayout = '[module]/-/[module]-[revision].[ext]'
                        repo.mavenCompatible = false
                        repo.repoKey = upmExtension.upmRepositoryKey.get()
                        repo.username = upmExtension.username.orNull
                        repo.password = upmExtension.password.orNull
                    }
                }
            }
            //This have to be resolved outside of the configuration block.
            def publicationsArray = publications.get().toArray()
            project.tasks.withType(ArtifactoryTask).configureEach { defaultTask ->
                defaultTask.publications(*publicationsArray)
                defaultTask.publishArtifacts = false
                defaultTask.publishIvy = false
            }
        }
    }

    private static void setupPublishTasksDependencies(Project project) {
        def artifactoryPublishTask = project.tasks.named(ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME)
        def publishTask = project.tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
        publishTask.configure { it.dependsOn(artifactoryPublishTask) }

        if (project.rootProject != project && project.rootProject.plugins.hasPlugin(PublishingPlugin)) {
            project.rootProject.tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) {
                it.dependsOn(publishTask)
            }
        }
    }
}
