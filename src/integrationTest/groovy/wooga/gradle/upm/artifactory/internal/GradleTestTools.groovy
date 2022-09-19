package wooga.gradle.upm.artifactory.internal

class GradleTestTools {

    static File initializeSubproject(File settingsFile, File subprojectDir) {
        subprojectDir.mkdirs()
        def buildFile = new File(subprojectDir, "build.gradle")
        buildFile.createNewFile()
        settingsFile << """
        include ':${subprojectDir.toPath().fileName.toString()}'
        """
        return buildFile
    }

}
