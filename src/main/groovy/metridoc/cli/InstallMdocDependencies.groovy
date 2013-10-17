package metridoc.cli

import groovy.sql.Sql
import metridoc.utils.ArchiveMethods

import java.util.concurrent.CountDownLatch


class InstallMdocDependencies {

    static void downloadDependencies () {
        File destination = getDestination()
        Set<String> currentLibs = getCurrentLibs(destination)

        def classLoader = Thread.currentThread().contextClassLoader
        def dependencyFile = classLoader.getResourceAsStream("DEPENDENCY_URLS").getText("utf-8").trim()

        doGrabDependencies(destination, dependencyFile)
        addDependenciesToClassPath(destination, currentLibs)
    }

    protected static void doGrabDependencies(File destination, String dependencyUrls) {

        print "Downloading Dependencies"

        def latch = new CountDownLatch(1)

        Throwable badDownloadError
        Thread.start {
            try {
                dependencyUrls.eachLine {String line ->
                    new URL(line).openConnection().with {
                        def jarFile = new File(destination, getFileName(line))
                        jarFile << inputStream
                        inputStream.close()
                    }
                }
            }
            catch (Throwable throwable) {
                badDownloadError = throwable
            }

            latch.countDown()
        }

        while (latch.count != 0) {
            print "."
            Thread.sleep(200)
        }

        println "" //print blank space

        if (badDownloadError) {
            throw badDownloadError
        }
    }

    protected static Set<String> getCurrentLibs(File destination) {
        Set currentLibs = [] as Set<String>
        destination.eachFile {
            currentLibs << it.name
        }
        currentLibs
    }

    protected static File getDestination() {
        def classpath = System.getProperty("java.class.path")
        def destination = new File(classpath.split(":")[0]).parentFile
        destination
    }

    protected static void addDependenciesToClassPath(File destination, currentLibs) {
        def classLoader = Sql.classLoader
        while (!(classLoader instanceof URLClassLoader)) {
            classLoader = classLoader.parent
        }

        destination.eachFile {
            if (!currentLibs.contains(it.name)) {
                classLoader.addURL(it.toURI().toURL())
            }
        }
    }

    protected static String getFileName(String url) {
        int index = url.lastIndexOf("/")
        url.substring(index + 1)
    }
}

