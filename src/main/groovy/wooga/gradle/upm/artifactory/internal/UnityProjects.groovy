package wooga.gradle.upm.artifactory.internal

import org.gradle.api.logging.Logger

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

class UnityProjects {

    static Map<Path, Path> findMetafiles(File baseDir) {
        def files = new HashMap<Path, Path>()
        def metafiles = new HashMap<Path, Path>()
        def folderFiles = Files.walk(baseDir.toPath())
        def dirContentsStream = folderFiles.parallel().filter {it != baseDir.toPath() }.sorted()
        dirContentsStream.each {
            it.toString().endsWith(".meta")? metafiles.put(it, it) : files.put(it, null)
        }

        return files.collectEntries(files) {
            def candidateMetafile = it.key.parent.resolve("${it.key.fileName.toString()}.meta")
            return [(it.key): (candidateMetafile.toFile().file? metafiles[candidateMetafile] : null)]
        }
    }

    static List<Path> filesWithoutMetafile(File packageDir) {
        def fileMetafile = findMetafiles(packageDir)
        return fileMetafile.findAll {it.value == null}.collect {it.key}
    }

    static Path findUPMPackageDirectory(File baseDir, Logger logger=null) {
        def manifestFiles = Files.walk(baseDir.toPath())
                .parallel()
                .filter({ Path path -> path.fileName.toString() == "package.json" })
                .collect(Collectors.toList())
        def upmBasePath = manifestFiles[0]?.parent
        switch (manifestFiles.size()) {
            case 1:
                return upmBasePath.toAbsolutePath()
            case 0:
                logger?.warn("No package manifest files (package.json) were found")
                break
            default:
                logger?.warn("More then one package manifest file (package.json) was found, using ${upmBasePath.toAbsolutePath()}")
                return upmBasePath.toAbsolutePath()
        }
        return null
    }

}
