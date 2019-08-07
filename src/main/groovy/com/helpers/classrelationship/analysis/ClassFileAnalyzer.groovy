package com.helpers.classrelationship.analysis

import org.apache.bcel.classfile.ConstantPool
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.Type

class ClassFileAnalyzer {

    private final JavaClass forClass
    private final Internals internals

    ClassFileAnalyzer(JavaClass forClass) {
        println("CLASS " + forClass.getClassName())
        this.forClass = forClass
        this.internals = new Internals(forClass)
    }

    JavaClass get() {
        return forClass
    }

    Internals internals() {
        return internals
    }

    Optional<Method> findMethod(String methodName, Type[] argumentTypes) {
        return findMethodInClass(methodName, argumentTypes) |
                { findMethodInSuperClasses(methodName, argumentTypes) }
    }

    Optional<Method> findMethodInClass(String methodName, Type[] argumentTypes) {
        return forClass.getMethods().toList().stream()
                .filter {isSameMethod(it, methodName, argumentTypes)}
                .findFirst()
    }


    Optional<Method> findMethodInSuperClasses( String methodName, Type[] argumentTypes) {
        return forClass.getSuperClasses().toList().stream()
                .map {new ClassFileAnalyzer(it).findMethod(methodName, argumentTypes)}
                .filter {it.isPresent()}
                .map {it.get()}
                .findFirst()
    }

    static boolean isSameMethod(Method method, String methodName, Type[] argumentTypes) {
        return method.getName() == methodName && Arrays.equals(argumentTypes, method.getArgumentTypes())
    }

    static class Internals {

        final ConstantPool constPool
        final ConstantPoolGen constPoolGen

        Internals(JavaClass javaClass) {
            this.constPool = javaClass.getConstantPool()
            this.constPoolGen = new ConstantPoolGen(this.constPool)
        }
    }
}

