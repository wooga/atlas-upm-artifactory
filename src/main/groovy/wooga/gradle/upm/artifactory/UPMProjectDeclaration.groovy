package wooga.gradle.upm.artifactory

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import wooga.gradle.unity.UnityPluginExtension
import wooga.gradle.unity.tasks.GenerateUpmPackage
import wooga.gradle.upm.artifactory.internal.Extensions
import wooga.gradle.upm.artifactory.internal.UPMProjectConfigurator
import wooga.gradle.upm.artifactory.traits.UPMPackSpec

import java.util.function.Consumer

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
            packageDirectory.convention(UPMArtifactoryConventions.resolvePackageDirectory(project, name))
            generateMetaFiles.convention(UPMArtifactoryConventions.generateMetaFiles.resolve(name).getBooleanValueProvider(project))
        }
        return extension
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
