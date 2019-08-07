package com.helpers.classrelationship.neo4j.persistor.entity

import com.google.common.collect.ImmutableList
import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.JarRegistry
import com.helpers.classrelationship.neo4j.CodeLabels
import com.helpers.classrelationship.neo4j.CodeRelationships
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor
import com.helpers.classrelationship.neo4j.persistor.AbstractPersistor.PersistStage
import com.helpers.classrelationship.neo4j.persistor.Constants
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.Type
import org.neo4j.unsafe.batchinsert.BatchInserter

class ClassPersistor extends AbstractPersistor<String, ClassRegistry.ClassDto> {

    private final ClassRegistry classRegistry

    ClassPersistor(int poolSize, JarRegistry jarRegistry, ClassRegistry classRegistry,
                   BatchInserter inserter) {
        super(poolSize, ImmutableList.of(
                new EntityPersistor("Class entity", inserter),
                new InterfaceSuperRelPersistor("Interfaces and extend", inserter, classRegistry),
                new FieldsPersistor("Class fields", inserter, classRegistry),
                new MethodPersistor("Class methods", inserter, classRegistry),
                new OverridesRelPersistor("Method overrides relationship", inserter, classRegistry),
                new JarRelPersistor("Class - JAR relationship", inserter, jarRegistry)
        ))

        this.classRegistry = classRegistry
    }

    @Override
    Map<String, ClassRegistry.ClassDto> getObjectsByKey() {
        return classRegistry.getRegistry()
    }

    private static String extractSimpleName(String fullName) {
        try {
            return (fullName =~ '([A-Za-z0-9_\\-$]+)$')[0][1]
        } catch (IndexOutOfBoundsException ex) {
            println "Failed to extract simple name for '$fullName'"
            return fullName
        }
    }

    private static class EntityPersistor extends PersistStage<String, ClassRegistry.ClassDto, ClassRegistry.ClassDto> {

        EntityPersistor(String name, BatchInserter batchInserter) {
            super(name, batchInserter)
        }

        @Override
        void doPersist(String className, ClassRegistry.ClassDto original, ClassRegistry.ClassDto analyzed) {
            def simpleName = analyzed.assignedClass.className.substring(
                    analyzed.assignedClass.packageName.length(),
                    analyzed.assignedClass.className.length())
                    .replaceAll('\\.', "")

            def id = inserter.createNode([
                    (Constants.Class.SIMPLE_NAME): simpleName,
                    (Constants.Class.NAME)       : analyzed.assignedClass.className,
                    (Constants.Class.PACKAGE)    : analyzed.assignedClass.packageName
            ], analyzed.assignedClass.interface ? CodeLabels.Labels.Interface : CodeLabels.Labels.Class)
            analyzed.entityId = id
        }
    }

    private static class InterfaceSuperRelPersistor
            extends PersistStage<String, ClassRegistry.ClassDto, ClassRegistry.ClassDto> {

        private final ClassRegistry classRegistry

        InterfaceSuperRelPersistor(String name, BatchInserter batchInserter, ClassRegistry classRegistry) {
            super(name, batchInserter)
            this.classRegistry = classRegistry
        }

        @Override
        void doPersist(String className, ClassRegistry.ClassDto original, ClassRegistry.ClassDto analyzed) {
            // only directly visible interfaces and super classes
            def extend = analyzed.assignedClass.getSuperclassName()
            createExtendsRel(analyzed, extend)

            analyzed.assignedClass.getInterfaceNames().toList()
                    .forEach { iface -> createExtendsRel(analyzed, iface) }
        }

        private void createExtendsRel(ClassRegistry.ClassDto analyzed, String extendsName) {
            def extend = extendsName
            def clazz = classRegistry.get(extend) ?: classRegistry.getUnresolved()

            inserter.createRelationship(analyzed.entityId, clazz.entityId, CodeRelationships.Relationships.EXTENDS, [
                    (Constants.Class.CLASS): extend,
                    (Constants.Class.SIMPLE_NAME): extractSimpleName(extend)
            ])

            inserter.createRelationship(clazz.entityId, analyzed.entityId, CodeRelationships.Relationships.IS_A, [
                    (Constants.Class.CLASS): extend,
                    (Constants.Class.SIMPLE_NAME): extractSimpleName(extend)
            ])
        }
    }

    private static class FieldsPersistor extends PersistStage<String, ClassRegistry.ClassDto, ClassRegistry.ClassDto> {

        private final ClassRegistry classRegistry

        FieldsPersistor(String name, BatchInserter batchInserter, ClassRegistry classRegistry) {
            super(name, batchInserter)
            this.classRegistry = classRegistry
        }

        @Override
        void doPersist(String className, ClassRegistry.ClassDto original, ClassRegistry.ClassDto analyzed) {
            analyzed.assignedClass.fields.toList().forEach { field ->
                def type = field.getType()
                def name = field.getName()
                def id = inserter.createNode([
                        (Constants.Field.NAME): name,
                        (Constants.Field.TYPE): field.getType().toString(),
                        (Constants.Field.OWNER_SIMPLE_NAME): extractSimpleName(className)
                ], CodeLabels.Labels.Field)

                analyzed.fields[name] = id

                // persist relations only for non-primitive fields
                if (type instanceof ObjectType) {
                    def asObj = (ObjectType) type
                    def clazz = classRegistry.get(asObj.className) ?: classRegistry.getUnresolved()

                    inserter.createRelationship(analyzed.entityId, id, CodeRelationships.Relationships.HAS, [:])
                    inserter.createRelationship(id, clazz.entityId, CodeRelationships.Relationships.IS, [:])
                }
            }
        }
    }

    private static class MethodPersistor extends PersistStage<String, ClassRegistry.ClassDto, ClassRegistry.ClassDto> {

        private final ClassRegistry classRegistry

        MethodPersistor(String name, BatchInserter batchInserter, ClassRegistry classRegistry) {
            super(name, batchInserter)
            this.classRegistry = classRegistry
        }

        @Override
        void doPersist(String className, ClassRegistry.ClassDto original, ClassRegistry.ClassDto analyzed) {
            analyzed.assignedClass.methods.toList().forEach { method ->
                def returns = method.getReturnType()
                def name = method.getName()
                def args = method.getArgumentTypes()

                def methodId = inserter.createNode([
                        (Constants.Method.NAME)             : name,
                        (Constants.Method.OWNER_SIMPLE_NAME): extractSimpleName(analyzed.assignedClass.className),
                        (Constants.Method.RETURN)           : returns.toString(),
                        (Constants.Method.ARGS)             : args.toString(),
                        (Constants.Method.IS_LAMBDA)        : method.isSynthetic()
                ], CodeLabels.Labels.Method)

                analyzed.methods[new ClassRegistry.MethodKey(name, args)] = methodId

                if (returns instanceof ObjectType) {
                    def asObj = (ObjectType) returns
                    def clazz = classRegistry.get(asObj.className) ?: classRegistry.getUnresolved()
                    inserter.createRelationship(methodId, clazz.entityId, CodeRelationships.Relationships.RETURNS, [:])
                }

                inserter.createRelationship(analyzed.entityId, methodId, CodeRelationships.Relationships.HAS, [:])
                inserter.createRelationship(methodId, analyzed.entityId, CodeRelationships.Relationships.IS_IN, [:])

                persistMethodArgs(args, methodId)
            }
        }

        private persistMethodArgs(Type[] args, long methodId) {
            args.toList().eachWithIndex { arg, index ->
                def argId = inserter.createNode([
                        (Constants.Method.Arg.NAME): index,
                        (Constants.Method.Arg.TYPE): arg.toString()
                ], CodeLabels.Labels.Argument)

                inserter.createRelationship(methodId, argId, CodeRelationships.Relationships.ARGUMENT, [:])

                // persist relations only for non-primitive fields
                if (arg instanceof ObjectType) {
                    def asObj = (ObjectType) arg
                    def clazz = classRegistry.get(asObj.className) ?: classRegistry.getUnresolved()
                    inserter.createRelationship(argId, clazz.entityId, CodeRelationships.Relationships.IS, [:])
                }
            }
        }
    }

    private static class OverridesRelPersistor extends PersistStage<String, ClassRegistry.ClassDto, ClassRegistry.ClassDto> {

        private final ClassRegistry classRegistry

        OverridesRelPersistor(String name, BatchInserter batchInserter, ClassRegistry classRegistry) {
            super(name, batchInserter)
            this.classRegistry = classRegistry
        }

        @Override
        void doPersist(String className, ClassRegistry.ClassDto original, ClassRegistry.ClassDto analyzed) {
            analyzed.assignedClass.methods.toList()
                    .forEach { method -> persistOverridesIfNeeded(method, analyzed) }
        }

        // Will persist indirect overrides too, i.e. empty interface that extends some other interface
        private void persistOverridesIfNeeded(Method method, ClassRegistry.ClassDto analyzed) {
            def key = new ClassRegistry.MethodKey(method.name, method.argumentTypes)
            def methodId = analyzed.methods[key]

            classRegistry.expandAllSuperclasses(analyzed).forEach {it -> createOverridesRelIfNeeded(methodId, key, it) }

            // persist indirect overrides too
            classRegistry.expandAllInterfaces(analyzed).forEach {it -> createOverridesRelIfNeeded(methodId, key, it)}
        }

        private void createOverridesRelIfNeeded(long methodId, ClassRegistry.MethodKey forMethod, String parentName) {
            def clazz = classRegistry.get(parentName) ?: classRegistry.getUnresolved()

            if (!clazz.methods.containsKey(forMethod)) {
                return
            }

            long parentMethodId = clazz.methods[forMethod]

            inserter.createRelationship(methodId, parentMethodId, CodeRelationships.Relationships.OVERRIDES, [:])
            inserter.createRelationship(parentMethodId, methodId, CodeRelationships.Relationships.OVERRIDDEN_BY, [:])
        }


    }

    private static class JarRelPersistor extends PersistStage<String, ClassRegistry.ClassDto, ClassRegistry.ClassDto> {

        private final JarRegistry jarRegistry

        JarRelPersistor(String name, BatchInserter batchInserter, JarRegistry jarRegistry) {
            super(name, batchInserter)
            this.jarRegistry = jarRegistry
        }

        @Override
        void doPersist(String className, ClassRegistry.ClassDto original, ClassRegistry.ClassDto analyzed) {
            analyzed.jarFilePathsAndHash.forEach {jar, hash ->
                def jarDto = jarRegistry.getByPath(jar) ?: jarRegistry.getUnresolved()
                def appName = analyzed.jarFilePathApp.get(jar)
                inserter.createRelationship(jarDto.entityId, analyzed.entityId, CodeRelationships.Relationships.PACKS, [
                        (Constants.Class.JAR): jar,
                        (Constants.Class.HASH): hash,
                        (Constants.Class.APP): appName
                ])
            }
        }
    }
}

