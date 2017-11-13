package com.helpers.classrelationship.neo4j.persistor.calls

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.ClassRegistry.MethodKey
import com.helpers.classrelationship.analysis.method.MethodAnalyzer
import com.helpers.classrelationship.analysis.method.finegrained.ClassNameReferenceAnalyzer
import com.helpers.classrelationship.analysis.method.finegrained.ExternalCallAnalyzer
import com.helpers.classrelationship.analysis.method.finegrained.FieldCallAnalyzer
import com.helpers.classrelationship.analysis.method.finegrained.InMethodBodyAction
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor.PersistStage
import com.helpers.classrelationship.neo4j.persistor.calls.clazz.NameReferencerPersistor
import com.helpers.classrelationship.neo4j.persistor.calls.external.ExternalCallPersistor
import com.helpers.classrelationship.neo4j.persistor.calls.fields.FieldsCallPersistor
import org.neo4j.unsafe.batchinsert.BatchInserter

class MethodBodyActionsPersistor extends AbstractPersistor<String, ClassRegistry.ClassDto> {

    private final ClassRegistry classRegistry

    MethodBodyActionsPersistor(int poolSize, ClassRegistry classRegistry,
                               BatchInserter inserter) {
        super(poolSize, ImmutableList.of(
                new Persistor("Method body actions", inserter, classRegistry)
        ))

        this.classRegistry = classRegistry
    }

    @Override
    Map<String, ClassRegistry.ClassDto> getObjectsByKey() {
        return classRegistry.getRegistry()
    }

    private static class Persistor extends PersistStage<String, ClassRegistry.ClassDto,
            Map<MethodKey, List<InMethodBodyAction>>> {

        private final ClassRegistry classes

        private final Map<Class, AbstractInMethodActionPersistor> dispatchers = ImmutableMap.builder()
                .put(ExternalCallAnalyzer.ExternalMethodCallDto.class, new ExternalCallPersistor(inserter, classes))
                .put(FieldCallAnalyzer.FieldCallDto.class, new FieldsCallPersistor(inserter, classes))
                .put(ClassNameReferenceAnalyzer.ClassNameReferenceDto.class, new NameReferencerPersistor(inserter, classes))
                .build()

        Persistor(String name, BatchInserter batchInserter, ClassRegistry classes) {
            super(name, batchInserter)
            this.classes = classes
        }

        @Override
        protected Map<MethodKey, List<InMethodBodyAction>> doAnalyze(String className, ClassRegistry.ClassDto clazz) {
            def analyzer = new ClassFileAnalyzer(clazz.assignedClass)

            return clazz.assignedClass.methods.toList().collectEntries { method ->
                [(new MethodKey(method.getName(), method.getArgumentTypes())):
                         new MethodAnalyzer(analyzer, method).analyze()]
            }
        }

        @Override
        protected void doPersist(String keyName, ClassRegistry.ClassDto clazz,
                                 Map<MethodKey, List<InMethodBodyAction>> instructionsByMethod) {

            instructionsByMethod.forEach { method, instructions ->
                def actionsByDispatcher = instructions.groupBy {dispatchers.get(it.getClass())}

                actionsByDispatcher.forEach {dispatcher, calls ->
                    dispatcher?.persist(clazz.methods[method], classes, calls)
                }
            }
        }
    }
}
