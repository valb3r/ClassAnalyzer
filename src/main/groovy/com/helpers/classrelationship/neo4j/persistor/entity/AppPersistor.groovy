package com.helpers.classrelationship.neo4j.persistor.entity

import com.helpers.classrelationship.analysis.AppRegistry
import com.helpers.classrelationship.neo4j.CodeLabels
import com.helpers.classrelationship.neo4j.persistor.Constants
import org.neo4j.unsafe.batchinsert.BatchInserter

class AppPersistor {

    private final AppRegistry appRegistry
    private final BatchInserter inserter

    AppPersistor(AppRegistry appRegistry, BatchInserter inserter) {
        this.appRegistry = appRegistry
        this.inserter = inserter
    }

    void persist() {
        appRegistry.getRegistry().forEach { appName, appDesc ->
            def id = inserter.createNode([
                    (Constants.App.NAME): appDesc.name,
            ], CodeLabels.Labels.App)
            appDesc.entityId = id
        }
    }
}
