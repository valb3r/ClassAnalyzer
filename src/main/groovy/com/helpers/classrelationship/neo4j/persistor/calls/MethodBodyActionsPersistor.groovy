package com.helpers.classrelationship.neo4j.persistor.calls

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.method.MethodAnalyzer
import com.helpers.classrelationship.analysis.MethodRegistry
import com.helpers.classrelationship.analysis.method.finegrained.ExternalCallAnalyzer
import com.helpers.classrelationship.analysis.method.finegrained.FieldCallAnalyzer
import com.helpers.classrelationship.analysis.method.finegrained.InMethodBodyAction
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor.PersistStage
import com.helpers.classrelationship.neo4j.persistor.calls.external.ExternalCallPersistor
import com.helpers.classrelationship.neo4j.persistor.calls.fields.FieldsCallPersistor
import org.neo4j.unsafe.batchinsert.BatchInserter

class MethodBodyActionsPersistor extends AbstractPersistor<MethodRegistry.Key, MethodRegistry.MethodDto> {

    private final MethodRegistry methods

    MethodBodyActionsPersistor(int poolSize, MethodRegistry methods, ClassRegistry classRegistry,
                               BatchInserter inserter) {
        super(poolSize, ImmutableList.of(
                new Persistor("Method body actions", inserter, classRegistry, methods)
        ))

        this.methods = methods
    }

    @Override
    Map<MethodRegistry.Key, MethodRegistry.MethodDto> getObjectsByKey() {
        return methods.getRegistry()
    }

    private static class Persistor extends PersistStage<MethodRegistry.Key, MethodRegistry.MethodDto,
            List<InMethodBodyAction>> {

        private final ClassRegistry classes
        private final MethodRegistry methods

        private final Map<Class, AbstractInMethodActionPersistor> dispatchers = ImmutableMap.builder()
                .put(ExternalCallAnalyzer.ExternalMethodCallDto, new ExternalCallPersistor(inserter, classes, methods))
                .put(FieldCallAnalyzer.FieldCallDto.class, new FieldsCallPersistor(inserter))
                .build()

        Persistor(String name, BatchInserter batchInserter, ClassRegistry classes, MethodRegistry methods) {
            super(name, batchInserter)
            this.classes = classes
            this.methods = methods
        }

        @Override
        protected List<InMethodBodyAction> doAnalyze(MethodRegistry.Key key, MethodRegistry.MethodDto methodDto) {
            def analyzer = new ClassFileAnalyzer(classes.get(key.className).assignedClass)

            return analyzer.findMethod(key.methodName, key.args)
                    .map {new MethodAnalyzer(methods, analyzer, it).analyze()}
                    .orElse([])
        }

        @Override
        protected void doPersist(MethodRegistry.Key key, MethodRegistry.MethodDto methodDto,
                                 List<InMethodBodyAction> actions) {
            def actionsByDispatcher = actions.groupBy {findDispatcher(it)}

            actionsByDispatcher.forEach {dispatcher, calls ->
                dispatcher?.persist(methodDto, calls)
            }
        }

        private AbstractInMethodActionPersistor findDispatcher(InMethodBodyAction instruction) {
            Class actionKind = dispatchers.keySet().find { it.isInstance(instruction) }
            return dispatchers.get(actionKind)
        }
    }
}
