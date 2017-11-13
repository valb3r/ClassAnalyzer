package com.helpers.classrelationship.neo4j.indexer

import com.helpers.classrelationship.neo4j.CodeLabels
import com.helpers.classrelationship.neo4j.persistor.Constants
import org.neo4j.unsafe.batchinsert.BatchInserter

class Indexer {

    private final BatchInserter batchInserter;

    Indexer(BatchInserter batchInserter) {
        this.batchInserter = batchInserter
    }

    void createIndexes() {
        indexClasses()
        indexMethods()
    }

    private void indexClasses() {
        batchInserter.createDeferredSchemaIndex(CodeLabels.Labels.Class).on(Constants.Class.SIMPLE_NAME).create()
        batchInserter.createDeferredSchemaIndex(CodeLabels.Labels.Interface).on(Constants.Class.SIMPLE_NAME).create()
        batchInserter.createDeferredSchemaIndex(CodeLabels.Labels.Field).on(Constants.Field.TYPE).create()
    }

    private void indexMethods() {
        batchInserter.createDeferredSchemaIndex(CodeLabels.Labels.Method).on(Constants.Method.NAME).create()
        batchInserter.createDeferredSchemaIndex(CodeLabels.Labels.Argument).on(Constants.Method.Arg.TYPE).create()
    }
}
