package com.helpers.classrelationship.neo4j.persistor.calls

import com.helpers.classrelationship.analysis.ClassRegistry
import org.neo4j.unsafe.batchinsert.BatchInserter

abstract class AbstractInMethodActionPersistor<T> {

    protected final BatchInserter inserter

    AbstractInMethodActionPersistor(BatchInserter inserter) {
        this.inserter = inserter
    }

    abstract void persist(long originEntityId, ClassRegistry registry, List<T> calls)
}
