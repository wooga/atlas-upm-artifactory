package wooga.gradle.upm.artifactory.traits

import com.wooga.gradle.BaseSpec
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import wooga.gradle.upm.artifactory.internal.MapPropertyWithRightDefault
import wooga.gradle.upm.artifactory.internal.repository.UPMArtifactRepository

trait UPMPublishSpec implements BaseSpec {

    @Option(option = "repositories", description = """
    Repositories [name: UPMArtifactRepository] that be used as publishing target by atlas-upm-artifactory
    """)
    private final MapProperty<String, UPMArtifactRepository> repositories = new MapPropertyWithRightDefault<>(objects, String, UPMArtifactRepository)

    MapProperty<String, UPMArtifactRepository> getRepositories() {
        return repositories
    }

    void setRepositories(Provider<Map<String, UPMArtifactRepository>> repositories) {
        this.repositories.set(repositories)
    }

    void setRepositories(Map<String, UPMArtifactRepository> repositories) {
        this.repositories.set(repositories)
    }

    @Option(option = "repository", description = """
    The repository where the UPM package will be published. Must match of the UPM repositories declared on repositories or in the publishing extension.
    """)
    private final Property<String> repository = objects.property(String)

    @Input
    Property<String> getRepository() {
        return repository
    }

    void setRepository(Provider<String> repository) {
        this.repository.set(repository)
    }

    void setRepository(String repository) {
        this.repository.set(repository)
    }


    @Option(option = "username", description = """
    The username credential of the target upm repository.
    """)
    private final Property<String> username = objects.property(String)

    @Input
    Property<String> getUsername() {
        return username
    }

    void setUsername(Provider<String> username) {
        this.username.set(username)
    }

    void setUsername(String username) {
        this.username.set(username)
    }

    @Option(option = "password", description = """
    The password credential of the target upm repository.
    """)
    private final Property<String> password = objects.property(String)

    @Input
    Property<String> getPassword() {
        return password
    }

    void setPassword(Provider<String> password) {
        this.password.set(password)
    }

    void setPassword(String password) {
        this.password.set(password)
    }



}