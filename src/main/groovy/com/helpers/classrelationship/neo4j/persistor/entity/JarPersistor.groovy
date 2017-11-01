package com.helpers.classrelationship.neo4j.persistor.entity

import com.helpers.classrelationship.analysis.AppRegistry
import com.helpers.classrelationship.analysis.JarRegistry
import com.helpers.classrelationship.neo4j.CodeLabels
import com.helpers.classrelationship.neo4j.CodeRelationships
import org.neo4j.unsafe.batchinsert.BatchInserter

class JarPersistor {

    private final AppRegistry appRegistry
    private final JarRegistry jarRegistry
    private final BatchInserter inserter

    JarPersistor(AppRegistry appRegistry, JarRegistry jarRegistry, BatchInserter inserter) {
        this.appRegistry = appRegistry
        this.jarRegistry = jarRegistry
        this.inserter = inserter
    }

    void persist() {
        jarRegistry.getRegistry().forEach { jarName, jarDesc ->
            def id = inserter.createNode([
                    (Constants.Jar.NAME): jarDesc.name,
                    (Constants.Jar.PATHS): jarDesc.paths.toString()
            ], CodeLabels.Labels.Jar)
            jarDesc.entityId = id

            jarDesc.appNames.forEach {app ->
                def resovledApp = appRegistry.get(app) ?: appRegistry.getUnresolved()
                inserter.createRelationship(resovledApp.entityId, id, CodeRelationships.Relationships.Composed, [:])
            }
        }
    }
}

