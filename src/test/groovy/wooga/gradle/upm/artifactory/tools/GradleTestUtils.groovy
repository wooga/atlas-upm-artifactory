package wooga.gradle.upm.artifactory.tools

import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder

class GradleTestUtils {

    Project project

    GradleTestUtils(Project project) {
        this.project = project
    }

    public <T> T requireExtension(Class<T> tClass) {
        return project.extensions.getByType(tClass)
    }

    public <T> T evaluate(@DelegatesTo(Project) Closure<T> cls) {
        def result = cls(project)
        evaluate(project)
        return result
    }

    public void evaluate(Project project) {
        ((DefaultProject)project).evaluate()
    }
}
