package com.helpers.classrelationship.neo4j.persistor

import org.neo4j.unsafe.batchinsert.BatchInserter

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.function.Predicate

import static groovyx.gpars.GParsPool.withPool


abstract class AbstractPersistor<K, V> {

    private static final int STATISTICS_STEP = 1000

    protected final int poolSize
    protected final List<PersistStage> persistStages

    AbstractPersistor(int poolSize, List<PersistStage> persistStages) {
        this.poolSize = poolSize
        this.persistStages = persistStages
    }

    void persist(Function<K, Boolean> filter) {
        Map<K, V> idToObj = getObjectsByKey()
        int total = idToObj.size()
        def start = System.currentTimeMillis()
        def doneObjects = new AtomicInteger()

        persistStages.eachWithIndex { PersistStage stage, int i ->
            println "On stage #${i + 1} of ${persistStages.size()} - ${stage.name}"
            doneObjects.set(0)

            withPool(poolSize) {
                idToObj.keySet().toList().eachParallel {id ->
                    persistIfPossible(stage, id, idToObj, filter)
                    printPerformanceStats(stage.name, doneObjects.incrementAndGet(), total, start)
                }
            }
        }
    }

    abstract Map<K, V> getObjectsByKey()

    static <A> void persistIfPossible(PersistStage<K, V, A> doPersist, K id, Map<K, V> idToObj,
                                      Function<K, Boolean> filter) {
        if (filter.apply(id)) {
            doPersist.persist(id, idToObj.get(id))
        }
    }

    static void printPerformanceStats(String stage, int doneObjects, int total, long start) {
        if (0 != doneObjects && doneObjects % STATISTICS_STEP == 0) {
            def analyzed = (float) doneObjects
            def moment = System.currentTimeMillis()
            def duration = (float) (moment - start) ?: 1.0f
            def classPerSec = (float) analyzed / (duration / 1000.0)
            def leftObjects = (float) (total - doneObjects)
            println "${LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()} " +
                    "[$stage] Analyzed $doneObjects objects of $total [${(analyzed * 100.0 / total).round(2)} %] " +
                    "(ETA: ${(leftObjects / classPerSec).round(2)} s @ ${classPerSec.round(2)} objects/s))"
        }
    }

    abstract static class PersistStage<K, V, A> {

        protected final String name
        protected final BatchInserter inserter

        PersistStage(String name, BatchInserter batchInserter) {
            this.name = name
            this.inserter = batchInserter
        }

        void persist(K objectKey, V objectRef) {
            A analyzed = doAnalyze(objectKey, objectRef)
            synchronized (inserter) {
                doPersist(objectKey, objectRef, analyzed)
            }
        }

        protected A doAnalyze(K objectKey, V objectRef) {
            // override this if you want to do some time consuming call before persisting
            return objectRef
        }

        abstract protected void doPersist(K objectKey, V objectRef, A analyzedData)
    }
}

