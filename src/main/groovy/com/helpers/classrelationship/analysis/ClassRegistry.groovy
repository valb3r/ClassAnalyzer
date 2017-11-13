package com.helpers.classrelationship.analysis

import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.ClassGen
import org.apache.bcel.generic.Type

import java.util.concurrent.ConcurrentHashMap

class ClassRegistry implements RegistryInterface {

    private static final JavaClass UNRESOLVED_CLASS_OBJ = new ClassGen(
            "UnresolvedClassReference",
            "",
            "ThisClassDoesNotExists.java",
            0,
            new String[0]).getJavaClass()

    private final ClassDto unresolvedClass

    Map<String, ClassDto> registry = new ConcurrentHashMap<>()

    ClassRegistry() {
        unresolvedClass = add(JarRegistry.UNRESOLVED_JAR, AppRegistry.UNRESOLVED_APP, UNRESOLVED_CLASS_OBJ)
    }

    ClassDto add(String jarFilePath, String appName, JavaClass clazz) {
        def className = clazz.className
        def modEntry = registry.computeIfAbsent(className, {new ClassDto()})
        modEntry.jarFilePathApp.put(jarFilePath, appName)
        modEntry.jarFilePathsAndHash.put(jarFilePath, Integer.toHexString(Objects.hash(clazz)))
        modEntry.assignedClass = clazz
        return modEntry
    }

    ClassDto get(String name) {
        return registry.get(name)
    }

    ClassDto getUnresolved() {
        return unresolvedClass
    }

    static class ClassDto {

        Map<String, String> jarFilePathsAndHash = [:]
        Map<String, String> jarFilePathApp = [:]
        Map<String, Long> fields = [:]
        Map<MethodKey, Long> methods = [:]
        JavaClass assignedClass
        long entityId
    }

    static class MethodKey {

        final String name
        final Type[] args

        MethodKey(String name, Type[] args) {
            this.name = name
            this.args = args
        }

        @Override
        String toString() {
            return name + Arrays.toString(args)
        }

        @Override
        boolean equals(o) {
            return Objects.equals(name, o.name) &&
                    Objects.equals(Arrays.toString(args), Arrays.toString(o.args))
        }

        @Override
        int hashCode() {
            return Objects.hash(name, Arrays.toString(args))
        }
    }
}