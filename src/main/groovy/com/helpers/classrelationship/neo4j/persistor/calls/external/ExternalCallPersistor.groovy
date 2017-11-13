package com.helpers.classrelationship.neo4j.persistor.calls.external

import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.method.finegrained.ExternalCallAnalyzer
import com.helpers.classrelationship.neo4j.CodeRelationships
import com.helpers.classrelationship.neo4j.persistor.Constants
import com.helpers.classrelationship.neo4j.persistor.calls.AbstractInMethodActionPersistor
import org.neo4j.helpers.collection.Iterables
import org.neo4j.unsafe.batchinsert.BatchInserter

class ExternalCallPersistor extends AbstractInMethodActionPersistor<ExternalCallAnalyzer.ExternalMethodCallDto> {

    private final ClassRegistry classRegistry

    ExternalCallPersistor(BatchInserter inserter, ClassRegistry classRegistry) {
        super(inserter)
        this.classRegistry = classRegistry
    }

    @Override
    void persist(long originEntityId, ClassRegistry registry, List<ExternalCallAnalyzer.ExternalMethodCallDto> calls) {

        def resolvedCalls = calls.stream().map {
            def refClass = classRegistry.get(it.referencedClassName)
            def refMethod = refClass?.methods?.get(new ClassRegistry.MethodKey(it.methodName, it.argumentTypes))

            refClass && refMethod ? refMethod : null
        }.filter {null != it}
                .collect {it}

        def callsWithCount = resolvedCalls.groupBy {it}

        callsWithCount.forEach {id, mtdCalls ->
            inserter.createRelationship(originEntityId, Iterables.first(mtdCalls),
                    CodeRelationships.Relationships.Calls, [(Constants.Method.CALL_COUNT): mtdCalls.size()])
        }
    }
}
