package com.helpers.classrelationship.neo4j.persistor.calls.clazz

import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.method.finegrained.ClassNameReferenceAnalyzer
import com.helpers.classrelationship.neo4j.CodeRelationships
import com.helpers.classrelationship.neo4j.persistor.Constants
import com.helpers.classrelationship.neo4j.persistor.calls.AbstractInMethodActionPersistor
import org.neo4j.helpers.collection.Iterables
import org.neo4j.unsafe.batchinsert.BatchInserter

class NameReferencerPersistor extends AbstractInMethodActionPersistor<ClassNameReferenceAnalyzer.ClassNameReferenceDto> {

    private final ClassRegistry classRegistry

    NameReferencerPersistor(BatchInserter inserter, ClassRegistry classRegistry) {
        super(inserter)
        this.classRegistry = classRegistry
    }

    @Override
    void persist(long originEntityId, ClassRegistry registry,
                 List<ClassNameReferenceAnalyzer.ClassNameReferenceDto> calls) {

        def resolvedUses = calls.stream().map { classRegistry.get(it.referencedClassName)?.entityId}
                .filter {null != it}
                .collect {it}

        def callsWithCount = resolvedUses.groupBy {it}

        callsWithCount.forEach {id, clzCalls ->
            inserter.createRelationship(originEntityId, Iterables.first(clzCalls),
                    CodeRelationships.Relationships.UsesClassName, [(Constants.StaticReference.CALL_COUNT): clzCalls.size()])
        }
    }
}
