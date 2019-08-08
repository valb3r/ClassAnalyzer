package com.helpers.classrelationship.neo4j.persistor.calls.external

import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.method.finegrained.ExternalCallAnalyzer
import com.helpers.classrelationship.neo4j.CodeRelationships
import com.helpers.classrelationship.neo4j.persistor.Constants
import com.helpers.classrelationship.neo4j.persistor.calls.AbstractInMethodActionPersistor
import org.neo4j.unsafe.batchinsert.BatchInserter

class ExternalCallPersistor extends AbstractInMethodActionPersistor<ExternalCallAnalyzer.ExternalMethodCallDto> {

    private final ClassRegistry classRegistry

    ExternalCallPersistor(BatchInserter inserter, ClassRegistry classRegistry) {
        super(inserter)
        this.classRegistry = classRegistry
    }

    @Override
    void persist(long originEntityId, ClassRegistry registry, List<ExternalCallAnalyzer.ExternalMethodCallDto> calls) {

        def pos = -1
        Map<Long, List<Integer>> posByCall = new LinkedHashMap<>()

        for (def it : calls) {
            pos++
            def refClass = classRegistry.get(it.referencedClassName)
            if (!refClass) {
                continue
            }

            def refKey = new ClassRegistry.MethodKey(it.methodName, it.argumentTypes)
            def refMethod = refClass?.methods?.get(refKey)
            refMethod = refMethod ? refMethod : findFromParentClass(refClass, refKey)
            if (!refMethod) {
                continue
            }

            posByCall.computeIfAbsent(refMethod, {[]}).add(pos)
        }

        posByCall.forEach {id, positions ->
            inserter.createRelationship(originEntityId, id,
                    CodeRelationships.Relationships.CALLS, [
                    (Constants.Method.CALL_COUNT): positions.size(),
                    (Constants.Method.MIN_POSITION): Collections.min(positions),
                    (Constants.Method.POSITION): positions.join(",")
            ])
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
