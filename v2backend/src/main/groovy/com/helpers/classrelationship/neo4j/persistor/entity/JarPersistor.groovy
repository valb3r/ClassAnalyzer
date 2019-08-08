package com.helpers.classrelationship.neo4j.persistor.entity

import com.google.common.collect.ImmutableList
import com.helpers.classrelationship.analysis.AppRegistry
import com.helpers.classrelationship.analysis.JarRegistry
import com.helpers.classrelationship.neo4j.CodeLabels
import com.helpers.classrelationship.neo4j.CodeRelationships
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor.PersistStage
import com.helpers.classrelationship.neo4j.persistor.Constants
import org.neo4j.unsafe.batchinsert.BatchInserter

class JarPersistor extends AbstractPersistor<String, JarRegistry.JarDto> {

    private final JarRegistry jarRegistry

    JarPersistor(int poolSize, AppRegistry appRegistry, JarRegistry jarRegistry, BatchInserter inserter) {
        super(poolSize, ImmutableList.of(new Persistor("Jar persist", inserter, appRegistry)))
        this.jarRegistry = jarRegistry
    }

    @Override
    Map<String, JarRegistry.JarDto> getObjectsByKey() {
        jarRegistry.getRegistry()
    }

    private static class Persistor extends PersistStage<String, JarRegistry.JarDto, JarRegistry.JarDto> {

        private final AppRegistry appRegistry

        Persistor(String name, BatchInserter batchInserter, AppRegistry appRegistry) {
            super(name, batchInserter)
            this.appRegistry = appRegistry
        }

        @Override
        void doPersist(String objectId, JarRegistry.JarDto original, JarRegistry.JarDto analyzed) {
            def id = inserter.createNode([
                    (Constants.Jar.NAME) : analyzed.name,
                    (Constants.Jar.PATHS): analyzed.paths.toString()
            ], CodeLabels.Labels.Jar)
            analyzed.entityId = id

            analyzed.appNames.forEach {app ->
                def resovledApp = appRegistry.get(app) ?: appRegistry.getUnresolved()
                inserter.createRelationship(resovledApp.entityId, id, CodeRelationships.Relationships.COMPOSED, [:])
            }
        }
    }
}

