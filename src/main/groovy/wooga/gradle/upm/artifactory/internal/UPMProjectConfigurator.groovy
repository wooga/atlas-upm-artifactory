package wooga.gradle.upm.artifactory.internal


import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.tasks.TaskProvider
import wooga.gradle.unity.tasks.GenerateUpmPackage
import wooga.gradle.unity.tasks.Unity
import wooga.gradle.upm.artifactory.UPMArtifactoryPlugin
import wooga.gradle.upm.artifactory.UPMProjectDeclaration

class UPMProjectConfigurator {

    static final String GENERATE_UPM_PACKAGE_TASK_SUFFIX = "UpmPack"
    static final String ARCHIVE_CONFIGURATION_SUFFIX = "Upm"
    static final String ARTIFACT_SUFFIX = "UpmArtifact"
    static final String PUBLICATION_SUFFIX = "Upm"

    final Project project
    final UPMProjectDeclaration upmProject



    static Provider<Collection<UPMProjectConfigurator>> fromDeclarations(Project project, Provider<? extends Collection<UPMProjectDeclaration>> upmProjects) {
        return upmProjects.map {fromDeclarations(project, it) }
    }

    static Collection<UPMProjectConfigurator> fromDeclarations(Project project, Collection<UPMProjectDeclaration> upmProjects) {
        return upmProjects.collect {new UPMProjectConfigurator(project, it)}
    }

    UPMProjectConfigurator(Project project, UPMProjectDeclaration upmProject) {
        this.project = project
        this.upmProject = upmProject
    }

    Provider<Publication> publicationForSingleUpmProject(PublishingExtension publishingExt,
                                               TaskProvider<Unity> generateMetafiles,
                                               NamedDomainObjectProvider<Configuration> rootConfig) {
        def upmPack = createUpmPackTask(generateMetafiles)
        def upmArtifact = createUPMArtifactConfiguration(upmPack, rootConfig)
        def upmPublication = createUpmPublication(publishingExt, upmPack, upmArtifact)
        return upmPublication
    }

    boolean shouldGenerateMetafiles(Task task) {
        hasNotEnoughMetafiles(task.logger).getOrElse(false) || upmProject.generateMetaFiles.get()
    }

    TaskProvider<GenerateUpmPackage> createUpmPackTask(TaskProvider<Unity> generateMetafiles) {
        def basePluginConvention = project.provider { project.rootProject.convention.plugins["base"] as BasePluginConvention }
        def upmPack = project.tasks.register("$upmProject.name$GENERATE_UPM_PACKAGE_TASK_SUFFIX", GenerateUpmPackage) {
            it.group = UPMArtifactoryPlugin.GROUP
            it.packageDirectory.convention(upmProject.packageDirectory)
            it.archiveVersion.set(upmProject.version)
            //we need to set this explicitly for projects on subplugins.
            it.destinationDirectory.convention(basePluginConvention.flatMap { it.distsDirectory })
            it.dependsOn(generateMetafiles)
        }
        return upmPack
    }

    PublishArtifact createUPMArtifactConfiguration(TaskProvider<GenerateUpmPackage> upmPack,
                                                   NamedDomainObjectProvider<Configuration> rootConfig = null) {
        def archiveConfig = project.configurations.register("$upmProject.name$ARCHIVE_CONFIGURATION_SUFFIX")
        rootConfig?.configure {
            it.extendsFrom(archiveConfig.get())
        }
        def upmArtifact = project.artifacts.add(archiveConfig.name, upmPack) { ConfigurablePublishArtifact it ->
            it.name = "${upmProject.name}$ARTIFACT_SUFFIX"
        }
        return upmArtifact
    }

    Provider<Publication> createUpmPublication(PublishingExtension publishingExt, TaskProvider<GenerateUpmPackage> upmPack, PublishArtifact upmArtifact) {
        def publicationsContainer = project.objects.domainObjectContainer(IvyPublication, {
            publishingExt.publications.maybeCreate("$upmProject.name$PUBLICATION_SUFFIX", IvyPublication)
        })

        def upmPublication = publicationsContainer.register("$upmProject.name$PUBLICATION_SUFFIX") { IvyPublication publication ->
            publication.module = upmPack.flatMap { it.packageName }.getOrNull()
            publication.revision = upmPack.flatMap { it.archiveVersion }.getOrNull()
            publication.artifact(upmArtifact)
        }
        return upmPublication
    }

    private Provider<Boolean> hasNotEnoughMetafiles(Logger logger = null) {
        return upmProject.packageDirectory.asFile.map { File upmPackDir ->
            def filesWithoutMeta = new UnityProjects().filesWithoutMetafile(upmPackDir)
            if (filesWithoutMeta.size() > 0) {
                logger?.info("{} files found without corresponding metafile in {}", filesWithoutMeta.size(), upmPackDir.path)
            }
            return filesWithoutMeta.size() > 0
        }
    }
}