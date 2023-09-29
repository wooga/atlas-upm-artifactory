package wooga.gradle.upm.artifactory
import nebula.test.ProjectSpec
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables

class UPMProjectDeclarationSpec extends ProjectSpec {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables()

    def "Resolves Package directory in Project root folder"() {
        given:
        def fooRoot = "fooRoot"
        def fooProject = "foo"
        environmentVariables.set("UPM_${fooProject.toUpperCase()}_PACKAGE_DIR", fooRoot);
        when:
        def packageDir = UPMProjectDeclaration.resolvePackageDirectory(project, fooProject)
        then:
        packageDir.get().asFile.absolutePath == project.layout.projectDirectory.dir(fooRoot).asFile.absolutePath
    }
}
