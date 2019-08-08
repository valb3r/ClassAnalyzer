package com.helpers.classrelationship.analysis

import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.JavaClass

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import java.util.zip.ZipException

import static groovyx.gpars.GParsPool.withPool

class JarAnalyzer {

    private final Path jarPath
    private final int poolSize

    JarAnalyzer(Path jarPath, poolSize) {
        this.jarPath = jarPath
        this.poolSize = poolSize
    }

    Set<JavaClass> getClasses() {
        Set<JavaClass> foundClasses = ConcurrentHashMap.newKeySet()

        try {
            def jarFile = new JarFile(jarPath.toFile())

            withPool(poolSize) {
                jarFile.entries().eachParallel { clazz ->
                    if (clazz.getName().endsWith(".class")) {
                        ClassParser parser = new ClassParser(jarPath.toString(), clazz.getName())
                        JavaClass javaClass = parser.parse()
                        foundClasses.add(javaClass)
                    }
                }
            }
        } catch(ZipException ex) {
            println("ERROR: Failed parsiong $jarPath, but continuing process, reason $ex")
            return Collections.emptySet()
        }

        return foundClasses
    }
}
