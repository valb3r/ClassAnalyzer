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
            if (!refClass) {
                return null
            }

            def refKey = new ClassRegistry.MethodKey(it.methodName, it.argumentTypes)
            def refMethod = refClass?.methods?.get(refKey)

            if (refClass && refMethod) {
                return refMethod
            }

            return findFromParentClass(refClass, refKey)
        }.filter {null != it}
                .collect {it}

        def callsWithCount = resolvedCalls.groupBy {it}

        callsWithCount.forEach {id, mtdCalls ->
            inserter.createRelationship(originEntityId, Iterables.first(mtdCalls),
                    CodeRelationships.Relationships.Calls, [(Constants.Method.CALL_COUNT): mtdCalls.size()])
        }
    }

    private Long findFromParentClass(ClassRegistry.ClassDto classDto, ClassRegistry.MethodKey key) {
        Set<String> superOwners = new LinkedHashSet<>(classRegistry.expandAllSuperclasses(classDto))
        superOwners.addAll(classRegistry.expandAllInterfaces(classDto))

        return superOwners.stream()
                .map {classRegistry.get(it)}
                .filter {null != it}
                .map {it.methods.get(key)}
                .filter {null != it}
                .findFirst()
                .orElse(null)
    }
}
