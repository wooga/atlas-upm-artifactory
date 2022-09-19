package wooga.gradle.upm.artifactory.internal

import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.client.HttpResponseException
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.jfrog.artifactory.client.model.RepoPath
import wooga.gradle.unity.utils.PackageManifestBuilder

import java.util.stream.IntStream


//TODO: test this task and see why it gets stuck
//https://jenkins.atlas.wooga.com/blue/organizations/jenkins/wdk_unity%2Fwdk-unity-AsyncAwait/detail/PR-48/1/pipeline
class UPMTestTools {

    static final String WOOGA_ARTIFACTORY_BASE_URL = "https://wooga.jfrog.io/wooga"
    static final String WOOGA_ARTIFACTORY_CI_REPO = "atlas-upm-integrationTest"

    static final String DEFAULT_VERSION = "0.0.1"
    static final String DEFAULT_PACKAGE_NAME = testPkgName("default")
    static final String prefix = RandomStringUtils.randomAlphabetic(4)

    static String testPkgName(String name) {
        return "${prefix}$name"
    }

    Artifactory artifactory

    UPMTestTools(Artifactory artifactory = createTestArtifactoryClient()) {
        this.artifactory = artifactory
    }

    static Artifactory createTestArtifactoryClient() {
        def (username, password) = credentialsFromEnv()
        return ArtifactoryClientBuilder.create()
                .setUrl(WOOGA_ARTIFACTORY_BASE_URL)
                .setUsername(username)
                .setPassword(password)
                .build()
    }

    static Tuple2<String, String> credentialsFromEnv(String userEnv = "ATLAS_ARTIFACTORY_INTEGRATION_USER",
                                                     String pwdEnv = "ATLAS_ARTIFACTORY_INTEGRATION_PASSWORD") {
        def user = mustGetEnv(userEnv)
        def password = mustGetEnv(pwdEnv)
        return [user, password]
    }


    static String mustGetEnv(String envKey) {
        return Optional.ofNullable(System.getenv(envKey)).
                orElseThrow { -> new IllegalArgumentException("$envKey environment not present") }
    }

    static String artifactoryURL(String baseURL = WOOGA_ARTIFACTORY_BASE_URL, String repoName) {
        return "$baseURL/$repoName"
    }

    /**
     * Delete everything in given repo created within the execution time of this Spec
     * @param repoName
     */
    def cleanupArtifactoryRepoInRange(String repoName = WOOGA_ARTIFACTORY_CI_REPO, long fromMs, long toMs) {
        List<RepoPath> searchItems = artifactory.searches()
                .repositories(repoName)
                .artifactsCreatedInDateRange(fromMs, toMs)
                .artifactsByName("${prefix}*")
                .doSearch()

        for (RepoPath searchItem : searchItems) {
            deleteFromArtifactoryIfExists(repoName, searchItem.getItemPath())
        }
    }

    def deleteFromArtifactoryIfExists(String repoName, String itemPath) {
        try {
            artifactory.repository(repoName).delete(itemPath)
        } catch (HttpResponseException e) {
            if (e.statusCode == 404) {
                println("${itemPath} was already deleted by artifactory, skipping")
            } else {
                throw e
            }
        }
    }

    boolean hasPackageOnArtifactory(String repoName = WOOGA_ARTIFACTORY_CI_REPO, String artifactName = DEFAULT_PACKAGE_NAME, String artifactVersion = DEFAULT_VERSION) {
        def packages = retry({List it -> it.size() == 0}) {
            return artifactory.searches()
                    .repositories(repoName)
                    .artifactsByName(artifactName)
                    .version(artifactVersion)
                    .doSearch()
                    .findAll {it.itemPath.endsWith(".json") && it.itemPath.contains(artifactVersion)}
        }
        assert packages.size() > 0: "Could not find artifact `${artifactName}` on repository ${repoName}"
        return true
    }

    static File writeTestPackage(File baseDir, String packageDirectory, String packageName = DEFAULT_PACKAGE_NAME, String packageVersion = DEFAULT_VERSION, boolean hasMetafiles = true, int fileCount = 1) {
        packageName = testPkgName(packageName)
        def packageDir = new File(baseDir, packageDirectory)
        packageDir.mkdirs()
        def packageManifestFile = new File(packageDir, "package.json")
        packageManifestFile.createNewFile()
        if (hasMetafiles) {
            new File(packageDir, "package.json.meta").with {
                createNewFile()
                text = "META"
            }
        }
        packageManifestFile.write(new PackageManifestBuilder(packageName, packageVersion).build())
        IntStream.range(0, fileCount).forEach {
            def baseName = "Sample$fileCount".toString()
            new File(packageDir, "${baseName}.cs").with {
                createNewFile()
                text = "class ${baseName} {}"
            }
            if (hasMetafiles) {
                new File(packageDir, "${baseName}.cs.meta").with{
                    createNewFile()
                    text= "META"
                }
            }
        }
        return packageDir
    }

    public static <T> T retry(long timeoutMs = 60000, long intervalMs = 5000, Closure<Boolean> retryCondition, Closure<T> operation) {
        def startTime = System.currentTimeMillis()
        def result = operation()
        while(retryCondition.call(result)) {
            Thread.sleep(intervalMs)
            result = operation()
            if(System.currentTimeMillis() > startTime + timeoutMs) {
                return result
            }
        }
        return result
    }


}
