package wooga.gradle.upm.artifactory.traits

import com.wooga.gradle.BaseSpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import wooga.gradle.unity.traits.GenerateUpmPackageSpec

import java.util.function.Consumer

trait UPMPackSpec extends BaseSpec {

    @Option(option = "package-directory", description = """
    The directory where the UPM package sources of the WDK are located.
     At its root, it must contain a package manifest file (package.json) file.
    """)
    private final DirectoryProperty packageDirectory = objects.directoryProperty()

    @Input
    DirectoryProperty getPackageDirectory() {
        return packageDirectory
    }

    void setPackageDirectory(Provider<? extends Directory> directory) {
        packageDirectory.set(directory)
    }

    void setPackageDirectory(Directory directory) {
        packageDirectory.set(directory)
    }

    @Option(option = "version", description = """
    The version of the UPM package being created. 
    """)
    private final Property<String> version = objects.property(String)

    @Input
    Property<String> getVersion() {
        return version
    }

    void setVersion(Provider<String> version) {
        this.version.set(version)
    }

    void setVersion(String version) {
        this.version.set(version)
    }

    @Option(option = "generate-meta-files", description = """
    If the creation of unity metafiles should be forced.
    """)
    private final Property<Boolean> generateMetaFiles = objects.property(Boolean)
    @Input
    Property<Boolean> getGenerateMetaFiles() {
        return generateMetaFiles
    }

    void setGenerateMetaFiles(Provider<Boolean> value) {
        generateMetaFiles.set(value)
    }

    void setGenerateMetaFiles(Boolean value) {
        generateMetaFiles.set(value)
    }

    private final ListProperty<Consumer<GenerateUpmPackageSpec>> packageCustomizers = objects.listProperty(Consumer<GenerateUpmPackageSpec>)

    @Input
    @Optional
    ListProperty<Consumer<GenerateUpmPackageSpec>> getPackageCustomizers() {
        return packageCustomizers
    }

    void setPackageCustomizers(Provider<? extends Iterable<Consumer<GenerateUpmPackageSpec>>> patchers) {
        packageCustomizers.set(patchers)
    }

    void setPackageCustomizers(Iterable<Consumer<GenerateUpmPackageSpec>> patchers) {
        packageCustomizers.set(patchers)
    }

    void customizePackage(Consumer<GenerateUpmPackageSpec> patcher) {
        this.packageCustomizers.add(patcher)
    }
}
