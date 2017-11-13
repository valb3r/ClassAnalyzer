package com.helpers.classrelationship.neo4j.persistor.calls.external

import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.MethodRegistry;
import com.helpers.classrelationship.analysis.method.finegrained.ExternalCallAnalyzer;
import com.helpers.classrelationship.neo4j.CodeRelationships;
import com.helpers.classrelationship.neo4j.persistor.Constants;
import com.helpers.classrelationship.neo4j.persistor.calls.AbstractInMethodActionPersistor;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.unsafe.batchinsert.BatchInserter;

class ExternalCallPersistor extends AbstractInMethodActionPersistor<ExternalCallAnalyzer.ExternalMethodCallDto> {

    private final ClassRegistry classRegistry
    private final MethodRegistry methodRegistry

    ExternalCallPersistor(BatchInserter inserter, ClassRegistry classRegistry, MethodRegistry methodRegistry) {
        super(inserter)
        this.classRegistry = classRegistry
        this.methodRegistry = methodRegistry
    }

    @Override
    void persist(MethodRegistry.MethodDto method, List<ExternalCallAnalyzer.ExternalMethodCallDto> calls) {

        def resolvedCalls = calls.stream().map {
            def refClass = classRegistry.get(it.referencedClassName)
            def refMethod = methodRegistry.get(it.referencedClassName, it.methodName, it.argumentTypes)
            refClass && refMethod ? refMethod : null
        }.filter {null != it}
                .collect {it}

        def callsWithCount = resolvedCalls.groupBy {it.entityId}

        callsWithCount.forEach {id, call ->
            inserter.createRelationship(method.entityId, Iterables.first(call).entityId,
                    CodeRelationships.Relationships.Calls, [(Constants.Method.CALL_COUNT): call.size()])
        }
    }
}
