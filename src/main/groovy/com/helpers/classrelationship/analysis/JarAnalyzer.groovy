package com.helpers.classrelationship.analysis

import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.JavaClass

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

import static groovyx.gpars.GParsPool.withPool

class JarAnalyzer {

    private final Path jarPath
    private final int poolSize

    JarAnalyzer(Path jarPath, poolSize) {
        this.jarPath = jarPath
        this.poolSize = poolSize
    }

    Set<JavaClass> getClasses() {
        def jarFile = new JarFile(jarPath.toFile())
        Set<JavaClass> foundClasses = ConcurrentHashMap.newKeySet()

        withPool(poolSize) {
            jarFile.entries().eachParallel { clazz ->
                if (clazz.getName().endsWith(".class")) {
                    ClassParser parser = new ClassParser(jarPath.toString(), clazz.getName())
                    JavaClass javaClass = parser.parse()
                    foundClasses.add(javaClass)
                }
            }
        }

        return foundClasses
    }
}