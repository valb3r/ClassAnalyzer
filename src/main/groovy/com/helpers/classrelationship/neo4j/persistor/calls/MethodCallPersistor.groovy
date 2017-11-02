package com.helpers.classrelationship.neo4j.persistor.calls

import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.method.MethodAnalyzer
import com.helpers.classrelationship.analysis.MethodRegistry
import com.helpers.classrelationship.neo4j.CodeRelationships
import com.helpers.classrelationship.neo4j.persistor.entity.Constants
import org.neo4j.helpers.collection.Iterables
import org.neo4j.unsafe.batchinsert.BatchInserter

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

import static groovyx.gpars.GParsPool.withPool

class MethodCallPersistor {

    private final MethodRegistry methodRegistry
    private final ClassRegistry classRegistry
    private final BatchInserter inserter
    private final int poolSize

    MethodCallPersistor(MethodRegistry methodRegistry, ClassRegistry classRegistry, BatchInserter inserter, int poolSize) {
        this.methodRegistry = methodRegistry
        this.classRegistry = classRegistry
        this.inserter = inserter
        this.poolSize = poolSize
    }

    void persist(Set<String> classes) {
        int total = classes.size()
        def start = System.currentTimeMillis()
        def doneClasses = new AtomicInteger(0)
        withPool(poolSize) {
            classes.toList().eachParallel {clazzName ->
                def clazz = classRegistry.get(clazzName)
                if (null != clazz) {
                    analyzeClass(clazz)
                }

                printPerformanceStats(doneClasses.incrementAndGet(), total, start)
            }
        }
    }

    private void analyzeClass(ClassRegistry.ClassDto clazz) {
        def analyzer = new ClassFileAnalyzer(clazz.assignedClass)
        analyzer.get().getMethods().toList().each {
            def methodCalls = []
            try {
                methodCalls = new MethodAnalyzer(methodRegistry, analyzer, it).analyze()
            } catch (ex) {
                println "Caught exception analyzing method ${it.name} of ${clazz.assignedClass.className} - $ex"
            }

            persistMethodCalls(
                    methodRegistry.get(clazz.assignedClass.className, it.name, it.argumentTypes),
                    methodCalls)
        }
    }

    private void persistMethodCalls(MethodRegistry.MethodDto method, List<MethodAnalyzer.MethodDto> methodCalls) {
        if (null == method || methodCalls.isEmpty()) {
            return
        }

        def resolvedCalls = methodCalls.stream().map {
            def refClass = classRegistry.get(it.referencedClassName)
            def refMethod = methodRegistry.get(it.referencedClassName, it.methodName, it.argumentTypes)
            refClass && refMethod ? refMethod : null
        }.filter {null != it}
                .collect {it}

        def callsWithCount = resolvedCalls.groupBy {it.entityId}

        synchronized (inserter) {
            callsWithCount.forEach {id, call ->
                inserter.createRelationship(method.entityId, Iterables.first(call).entityId,
                        CodeRelationships.Relationships.Calls, [(Constants.Method.CALL_COUNT): call.size()])
            }
        }
    }

    private static void printPerformanceStats(int doneClasses, int total, long start) {
        if (0 != doneClasses && doneClasses % 100 == 0) {
            def analyzed = (float) doneClasses
            def moment = System.currentTimeMillis()
            def duration = (float) (moment - start) ?: 1.0f
            def classPerSec = (float) analyzed / (duration / 1000.0)
            def leftClasses = (float) (total - doneClasses)
            println "${LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()} " +
                    "Analyzed $doneClasses classes of $total [${(analyzed * 100.0 / total).round(2)} %] " +
                    "(ETA: ${(leftClasses / classPerSec).round(2)} s @ ${classPerSec.round(2)} class/s))"
        }
    }
}
