package wooga.gradle.upm.artifactory

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import wooga.gradle.upm.artifactory.internal.Extensions
import wooga.gradle.upm.artifactory.internal.UPMProjectConfigurator
import wooga.gradle.upm.artifactory.traits.UPMPackSpec

class UPMProjectDeclaration implements UPMPackSpec, Named {

    protected String name

    static UPMProjectDeclaration withStaticConventions(Project project, String name) {
        def extension = project.objects.newInstance(UPMProjectDeclaration).with {
            it.name = name
            return it
        }
        Extensions.setPropertiesOwner(UPMProjectDeclaration, extension, name)
        extension.with {
            version.convention(UPMArtifactoryConventions.version.resolve(name).getStringValueProvider(project).orElse(project.provider { project.version.toString() }))
            packageDirectory.convention(resolvePackageDirectory(project, name))
            generateMetaFiles.convention(UPMArtifactoryConventions.generateMetaFiles.resolve(name).getBooleanValueProvider(project))
        }
        return extension
    }

    static final Provider<Directory> resolvePackageDirectory(Project project, String name) {
        UPMArtifactoryConventions.packageDirectory.resolve(name)
                .getDirectoryValueProvider(project, null,
                        project.providers.provider({
                            project.layout.projectDirectory
                        }))
    }

    UPMProjectDeclaration() {}

    @Override
    String getName() {
        return name
    }

    def createConfigurator(Project project) {
        return new UPMProjectConfigurator(project, this)
    }

}
