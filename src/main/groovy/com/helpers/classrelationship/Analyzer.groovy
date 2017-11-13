package com.helpers.classrelationship

import com.helpers.classrelationship.analysis.AppRegistry
import com.helpers.classrelationship.analysis.JarAnalyzer
import com.helpers.classrelationship.analysis.JarRegistry
import com.helpers.classrelationship.analysis.JarRegistry.JarDto
import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.MethodRegistry
import com.helpers.classrelationship.neo4j.Neo4j
import com.helpers.classrelationship.neo4j.indexer.Indexer
import com.helpers.classrelationship.neo4j.persistor.calls.MethodBodyActionsPersistor
import com.helpers.classrelationship.neo4j.persistor.entity.AppPersistor
import com.helpers.classrelationship.neo4j.persistor.entity.ClassPersistor
import com.helpers.classrelationship.neo4j.persistor.entity.JarPersistor
import org.apache.bcel.classfile.JavaClass
import org.neo4j.unsafe.batchinsert.BatchInserter

/**
 * Loads java classes into database.
 * Arguments (positional):
 * args[0] - comma-separated list of directories with jars to analyze in format path1,path2=ProductName1;path3,path4=ProductName2, i.e. C:\project1\lib=Product1
 * args[1] - regex for class package to include in analysis (class entities and class-level relationship)
 * args[2] - regex for class package to exclude in analysis (class entities and class-level relationship)
 * args[3] - regex for class package to include in METHOD BODY analysis (detects calls from a method to external/this class)
 * args[4] - regex for class package to exclude in METHOD BODY analysis (detects calls from a method to external/this class)
 * args[5] - db file path
 */

final def DIRECTORIES = (args[0].split(";")).toList()
final def SMALL_POOL_SIZE = 1
final def POOL_SIZE = 1
final String REGEX_CLASS_INCLUDE = args[1]
final String REGEX_CLASS_EXCLUDE = args[2]
final String REGEX_METHOD_BODY_INCLUDE = args[3]
final String REGEX_METHOD_BODY_EXCLUDE = args[4]
final File DB = new File(args[5])
final Neo4j NEO4J = new Neo4j(DB)
final BatchInserter INSERTER = NEO4J.getInserter()

try {
    def APPS = new AppRegistry()
    def JARS = new JarRegistry()
    def CLASSES = new ClassRegistry()
    def METHODS = new MethodRegistry()

    def processJarClasses = { String jarPath, Set<JavaClass> found, JarDto jar, String product ->
        found.stream()
                .filter { it.className.matches(REGEX_CLASS_INCLUDE) && !it.className.matches(REGEX_CLASS_EXCLUDE) }
                .each { CLASSES.add(jarPath, product, it) }
    }

    Benchmark.method("Parse jars", "") {
        DIRECTORIES.forEach { dirsAndProduct ->
            def dirAndProductSplit = dirsAndProduct.split('=')
            def dirs = dirAndProductSplit[0].split(",")
            def product = dirAndProductSplit[1]
            def app = APPS.add(product)

            Set<File> jars = []
            dirs.each { dir ->
                new File(dir).eachFileRecurse { file ->
                    if (file.name.endsWith('.jar')) {
                        jars.add(file)
                    }
                }
            }

            jars.each { file ->
                def jar = JARS.add(file.toString(), app.name)
                println "Loading JAR $file"
                def found = new JarAnalyzer(file.toPath(), POOL_SIZE).getClasses()
                println "Found ${found.size()} classes in $file"
                processJarClasses(file.toString(), found, jar, product)
            }
        }
    }

    Benchmark.method("Persists applications", "${ APPS.registry.size() }") {
        new AppPersistor(SMALL_POOL_SIZE, APPS, INSERTER).persist({true})

    }

    Benchmark.method("Persists jars", "${ JARS.registry.size() }") {
        new JarPersistor(SMALL_POOL_SIZE, APPS, JARS, INSERTER).persist({true})
    }

    Benchmark.method("Persists classes", "${ CLASSES.registry.size() }") {
        new ClassPersistor(POOL_SIZE, JARS, CLASSES, METHODS, INSERTER).persist({true})
    }

    Benchmark.method("Persists method body calls", "Before filter ${ CLASSES.registry.size() }") {
        new MethodBodyActionsPersistor(POOL_SIZE, METHODS, CLASSES, INSERTER)
                .persist({
            it.className.matches(REGEX_METHOD_BODY_INCLUDE) && !it.className.matches(REGEX_METHOD_BODY_EXCLUDE)
        })
    }

    Benchmark.method("Indexing data", "deferred") {
        new Indexer(INSERTER).createIndexes()
    }

} finally {
    Benchmark.method("Batch inserter shutdown", "") {
        INSERTER.shutdown()
    }
}