package com.helpers.classrelationship.neo4j.persistor.calls.fields

import com.helpers.classrelationship.analysis.MethodRegistry;
import com.helpers.classrelationship.analysis.method.finegrained.FieldCallAnalyzer;
import com.helpers.classrelationship.neo4j.persistor.calls.AbstractInMethodActionPersistor;
import org.neo4j.unsafe.batchinsert.BatchInserter;

class FieldsCallPersistor extends AbstractInMethodActionPersistor<FieldCallAnalyzer.FieldCallDto> {

    FieldsCallPersistor(BatchInserter inserter) {
        super(inserter);
    }

    @Override
    void persist(MethodRegistry.MethodDto method, List<FieldCallAnalyzer.FieldCallDto> calls) {

    }
}
