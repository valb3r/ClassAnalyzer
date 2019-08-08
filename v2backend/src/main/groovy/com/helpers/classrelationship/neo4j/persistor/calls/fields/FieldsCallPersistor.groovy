package com.helpers.classrelationship.neo4j.persistor.calls.fields

import com.google.common.collect.ImmutableMap
import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.method.finegrained.FieldCallAnalyzer
import com.helpers.classrelationship.neo4j.CodeRelationships
import com.helpers.classrelationship.neo4j.persistor.Constants
import com.helpers.classrelationship.neo4j.persistor.calls.AbstractInMethodActionPersistor
import org.neo4j.graphdb.RelationshipType
import org.neo4j.helpers.collection.Iterables
import org.neo4j.unsafe.batchinsert.BatchInserter

class FieldsCallPersistor extends AbstractInMethodActionPersistor<FieldCallAnalyzer.FieldCallDto> {

    private final Map<FieldCallAnalyzer.FieldCallDto.Kind, RelationshipType> RELATIONSHIPS = ImmutableMap.of(
            FieldCallAnalyzer.FieldCallDto.Kind.PUT, CodeRelationships.Relationships.PUT,
            FieldCallAnalyzer.FieldCallDto.Kind.GET, CodeRelationships.Relationships.GET
    )

    private final ClassRegistry classRegistry

    FieldsCallPersistor(BatchInserter inserter, ClassRegistry classRegistry) {
        super(inserter)
        this.classRegistry = classRegistry
    }

    @Override
    void persist(long originEntityId, ClassRegistry registry, List<FieldCallAnalyzer.FieldCallDto> fields) {

        Map<FieldCallAnalyzer.FieldCallDto.Kind, List<Long>> callsByMode = [:]

        fields.stream().forEach {
            def refClass = classRegistry.get(it.referencedClassName)
            def refField = refClass?.fields?.get(it.fieldName)
            if (refClass && refField) {
                callsByMode.computeIfAbsent(it.kind, {[]}).add(refField)
            }
        }

        callsByMode.forEach { mode, byMode ->
            byMode.groupBy {it}.forEach {id, calls ->
                inserter.createRelationship(originEntityId, Iterables.first(calls),
                        RELATIONSHIPS[mode], [(Constants.Field.CALL_COUNT): calls.size()])
            }
        }
    }
}
