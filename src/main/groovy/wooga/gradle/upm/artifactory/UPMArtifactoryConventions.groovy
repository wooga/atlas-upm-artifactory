package wooga.gradle.upm.artifactory

import com.wooga.gradle.PropertyLookup
import com.wooga.gradle.PropertyUtils
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import wooga.gradle.upm.artifactory.internal.DynamicPropertyLookup

class UPMArtifactoryConventions {

    static final PropertyLookup repository = new PropertyLookup(
            ["UPM_REPOSITORY"],
            ["upm.repository", "publish.repository"],
            null
    )

    static final PropertyLookup username = new PropertyLookup(
            ["UPM_USR", "UPM_USERNAME"],
            ["upm.username", "publish.username"],
            null
    )

    static final PropertyLookup password = new PropertyLookup(
            ["UPM_PWD", "UPM_PASSWORD"],
            ["upm.password", "publish.password"],
            null
    )

    static final DynamicPropertyLookup<String> version = new DynamicPropertyLookup<>(
            { ["UPM_${PropertyUtils.envNameFromProperty(it)}_PACKAGE_VERSION" , "UPM_${PropertyUtils.envNameFromProperty(it)}_VERSION",
                              "UPM_PACKAGE_VERSION" , "UPM_VERSION"] },
            { ["upm.${it}.version" , "upm.${it}.package.version", "${it}.publish.version",
                            "upm.version" , "upm.package.version", "publish.version"] },
            { null }
    )


    static final DynamicPropertyLookup<String> packageDirectory = new DynamicPropertyLookup<>(
            { ["UPM_${PropertyUtils.envNameFromProperty(it)}_PACKAGE_DIR", "UPM_${PropertyUtils.envNameFromProperty(it)}_PACKAGE_DIRECTORY",
                              "UPM_PACKAGE_DIR", "UPM_PACKAGE_DIRECTORY"] },
            { ["upm.${it}.package.directory",
                           "upm.package.directory"] },
            { null }
    )

    static final DynamicPropertyLookup<String> generateMetaFiles = new DynamicPropertyLookup<>(
            { ["UPM_${PropertyUtils.envNameFromProperty(it)}_GENERATE_METAFILES" , "UPM_${PropertyUtils.envNameFromProperty(it)}_GENERATE_META_FILES",
                              "UPM_GENERATE_METAFILES" , "UPM_GENERATE_META_FILES"] },
            { ["upm.${it}.generate.metafiles",
                           "upm.generate.metafiles"] },
            { false }
    )

    static final Provider<Directory> resolvePackageDirectory(Project project, String name) {
        packageDirectory.resolve(name)
                .getDirectoryValueProvider(project, null, project.providers.provider{project.layout.projectDirectory})
    }

}
