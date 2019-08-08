package com.helpers.classrelationship.neo4j.persistor.entity

import com.google.common.collect.ImmutableList
import com.helpers.classrelationship.analysis.AppRegistry
import com.helpers.classrelationship.neo4j.CodeLabels
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor.PersistStage
import com.helpers.classrelationship.neo4j.persistor.Constants
import org.neo4j.unsafe.batchinsert.BatchInserter

class AppPersistor extends AbstractPersistor<String, AppRegistry.AppDto> {

    private final AppRegistry appRegistry
    private final BatchInserter inserter

    AppPersistor(int poolSize, AppRegistry appRegistry, BatchInserter inserter) {
        super(poolSize,ImmutableList.of(new Persistor("Application entity", inserter)))
        this.appRegistry = appRegistry
        this.inserter = inserter
    }

    @Override
    Map<String, AppRegistry.AppDto> getObjectsByKey() {
        appRegistry.getRegistry()
    }

    private static class Persistor extends PersistStage<String, AppRegistry.AppDto, AppRegistry.AppDto> {

        Persistor(String name, BatchInserter batchInserter) {
            super(name, batchInserter)
        }

        @Override
        void doPersist(String appName, AppRegistry.AppDto original, AppRegistry.AppDto analyzed) {
            def id = inserter.createNode([
                    (Constants.App.NAME): analyzed.name,
            ], CodeLabels.Labels.App)
            analyzed.entityId = id
        }
    }
}
