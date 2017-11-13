package com.helpers.classrelationship.neo4j.persistor.entity

import com.helpers.classrelationship.analysis.AppRegistry
import com.helpers.classrelationship.analysis.ClassRegistry
import com.helpers.classrelationship.analysis.JarRegistry
import com.helpers.classrelationship.analysis.MethodRegistry
import com.helpers.classrelationship.neo4j.CodeLabels
import com.helpers.classrelationship.neo4j.CodeRelationships
import com.helpers.classrelationship.neo4j.persistor.Constants
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.Type
import org.neo4j.unsafe.batchinsert.BatchInserter

class ClassPersistor {

    private final AppRegistry appRegistry
    private final JarRegistry jarRegistry
    private final ClassRegistry classRegistry
    private final MethodRegistry methodRegistry
    private final BatchInserter inserter

    ClassPersistor(AppRegistry appRegistry, JarRegistry jarRegistry, ClassRegistry classRegistry,
                   MethodRegistry methodRegistry, BatchInserter inserter) {
        this.appRegistry = appRegistry
        this.jarRegistry = jarRegistry
        this.classRegistry = classRegistry
        this.methodRegistry = methodRegistry
        this.inserter = inserter
    }

    void persist() {
        persistClassEntities()
        persistInterfaceSuperRel()
        persistFields()
        persistMethods()
        persistJarAndAppRel()
    }

    private void persistClassEntities() {
        classRegistry.getRegistry().forEach {className, classDesc ->
            def simpleName = classDesc.assignedClass.className.substring(
                    classDesc.assignedClass.packageName.length(),
                    classDesc.assignedClass.className.length())
                    .replaceAll('\\.', "")

            def id = inserter.createNode([
                    (Constants.Class.SIMPLE_NAME): simpleName,
                    (Constants.Class.NAME)       : classDesc.assignedClass.className,
                    (Constants.Class.PACKAGE)    : classDesc.assignedClass.packageName
            ], classDesc.assignedClass.interface ? CodeLabels.Labels.Interface : CodeLabels.Labels.Class)
            classDesc.entityId = id
        }
    }

    private void persistInterfaceSuperRel() {
        classRegistry.getRegistry().forEach { className, classDesc ->
            // only directly visible interfaces and super classes
            def extend = classDesc.assignedClass.getSuperclassName()
            def clazz = classRegistry.get(extend) ?: classRegistry.getUnresolved()
            inserter.createRelationship(classDesc.entityId, clazz.entityId, CodeRelationships.Relationships.Extends, [
                    (Constants.Class.CLASS): extend,
                    (Constants.Class.SIMPLE_NAME): extractSimpleName(extend)
            ])
            classDesc.assignedClass.getInterfaceNames().toList().forEach { iface ->
                def ifaceClazz = classRegistry.get(iface) ?: classRegistry.getUnresolved()
                inserter.createRelationship(classDesc.entityId, ifaceClazz.entityId, CodeRelationships.Relationships.Extends, [
                        (Constants.Class.INTERFACE): iface,
                        (Constants.Class.SIMPLE_NAME): extractSimpleName(iface)
                ])
            }
        }
    }

    private void persistFields() {
        classRegistry.getRegistry().forEach {className, classDesc ->
            classDesc.assignedClass.fields.toList().forEach { field ->
                def type = field.getType()
                def name = field.getName()
                def id = inserter.createNode([
                        (Constants.Field.NAME): name,
                        (Constants.Field.TYPE): field.getType().toString()
                ], CodeLabels.Labels.Field)

                // persist relations only for non-primitive fields
                if (type instanceof ObjectType) {
                    def asObj = (ObjectType) type
                    def clazz = classRegistry.get(asObj.className) ?: classRegistry.getUnresolved()

                    inserter.createRelationship(classDesc.entityId, id, CodeRelationships.Relationships.Has, [:])
                    inserter.createRelationship(id, clazz.entityId, CodeRelationships.Relationships.Is, [:])
                }
            }
        }
    }

    private void persistMethods() {
        classRegistry.getRegistry().forEach {className, classDesc ->
            classDesc.assignedClass.methods.toList().forEach { method ->
                def returns = method.getReturnType()
                def name = method.getName()
                def args = method.getArgumentTypes()

                def methodId = inserter.createNode([
                        (Constants.Method.NAME): name,
                        (Constants.Method.OWNER_SIMPLE_NAME): extractSimpleName(classDesc.assignedClass.className),
                        (Constants.Method.RETURN): returns.toString(),
                        (Constants.Method.ARGS): args.toString()
                ], CodeLabels.Labels.Method)

                methodRegistry.add(classDesc.assignedClass.className, name, args, methodId)

                if (returns instanceof ObjectType) {
                    def asObj = (ObjectType) returns
                    def clazz = classRegistry.get(asObj.className) ?: classRegistry.getUnresolved()
                    inserter.createRelationship(methodId, clazz.entityId, CodeRelationships.Relationships.Returns, [:])
                }

                inserter.createRelationship(classDesc.entityId, methodId, CodeRelationships.Relationships.Has, [:])

                persistMethodArgs(args, methodId)
            }
        }
    }

    private persistMethodArgs(Type[] args, long methodId) {
        args.toList().eachWithIndex { arg, index ->
            def argId = inserter.createNode([
                    (Constants.Method.Arg.NAME): index,
                    (Constants.Method.Arg.TYPE): arg.toString()
            ], CodeLabels.Labels.Argument)

            inserter.createRelationship(methodId, argId, CodeRelationships.Relationships.Argument, [:])

            // persist relations only for non-primitive fields
            if (arg instanceof ObjectType) {
                def asObj = (ObjectType) arg
                def clazz = classRegistry.get(asObj.className) ?: classRegistry.getUnresolved()
                inserter.createRelationship(argId, clazz.entityId, CodeRelationships.Relationships.Is, [:])
            }
        }
    }

    private void persistJarAndAppRel() {
        classRegistry.getRegistry().forEach { className, classDesc ->
            classDesc.jarFilePathsAndHash.forEach {jar, hash ->
                def jarDto = jarRegistry.getByPath(jar) ?: jarRegistry.getUnresolved()
                def appName = classDesc.jarFilePathApp.get(jar)
                inserter.createRelationship(jarDto.entityId, classDesc.entityId, CodeRelationships.Relationships.Packs, [
                        (Constants.Class.JAR): jar,
                        (Constants.Class.HASH): hash,
                        (Constants.Class.APP): appName
                ])
            }
        }
    }

    private static String extractSimpleName(String fullName) {
        try {
            return (fullName =~ '([A-Za-z0-9_\\-$]+)$')[0][1]
        } catch (IndexOutOfBoundsException ex) {
            println "Failed to extract simple name for '$fullName'"
            return fullName
        }
    }
}

