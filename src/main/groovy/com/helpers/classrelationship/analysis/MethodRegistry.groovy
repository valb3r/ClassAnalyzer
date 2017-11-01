package com.helpers.classrelationship.analysis

import org.apache.bcel.generic.Type

import java.util.concurrent.ConcurrentHashMap

class MethodRegistry implements RegistryInterface {

    Map<Key, MethodDto> registry = new ConcurrentHashMap<>()

    MethodDto get(String className, String methodName, Type[] args) {
        return registry.get(Key.build(className, methodName, args))
    }

    MethodDto add(String className, String methodName, Type[] args, long entityId) {
        def key = Key.build(className, methodName, args)
        return registry.put(key, new MethodDto([
                descriptor: key,
                entityId: entityId
        ]))
    }

    static class Key {

        String className
        String methodName
        Type[] args

        static build(String className, String methodName, Type[] args) {
            return new Key([
                    className: className,
                    methodName: methodName,
                    args: args
            ])
        }

        @Override
        String toString() {
            return className + methodName + Arrays.toString(args)
        }

        @Override
        boolean equals(o) {
            return Objects.equals(className, o.className) &&
                    Objects.equals(methodName, o.methodName) &&
                    Objects.equals(Arrays.toString(args), Arrays.toString(o.args))
        }

        @Override
        int hashCode() {
            return Objects.hash(className, methodName, Arrays.toString(args))
        }
    }

    static class MethodDto {

        Key descriptor
        long entityId
    }
}
