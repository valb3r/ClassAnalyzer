package com.helpers.classrelationship.analysis

import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.InstructionList
import org.apache.bcel.generic.InvokeInstruction
import org.apache.bcel.generic.MethodGen
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.ReferenceType
import org.apache.bcel.generic.Type

class MethodAnalyzer {

    private final MethodRegistry methodRegistry
    private final ClassFileAnalyzer classAnalyzer;
    private final Method method;

    MethodAnalyzer(MethodRegistry methodRegistry, ClassFileAnalyzer classAnalyzer, Method method) {
        this.methodRegistry = methodRegistry
        this.classAnalyzer = classAnalyzer
        this.method = method
    }

    List<MethodDto> getMethodCalls() {
        String fullClassName = classAnalyzer.get().getClassName()
        MethodGen mg = new MethodGen(method, fullClassName, classAnalyzer.internals().constPoolGen)
        InstructionList il = mg.getInstructionList()
        if (il == null) {
            return Collections.emptyList()
        }

        // limit to external class calls
        return il.getInstructionHandles().toList().stream().map {it.getInstruction()}
                .filter {it instanceof InvokeInstruction}
                .map {(InvokeInstruction) it}
                .filter {null != it}
                .map {buildFromInstruction(it)}
                .filter {null != it}
                .collect {it}
    }

    private MethodDto buildFromInstruction(InvokeInstruction invoke) {
        ConstantPoolGen constantPoolGen = classAnalyzer.internals().constPoolGen
        ReferenceType referenceType = invoke.getReferenceType(constantPoolGen)
        if (!(referenceType instanceof ObjectType)) {
            return null;
        }

        ObjectType objectType = (ObjectType) referenceType
        String referencedClassName = objectType.getClassName()

        String methodName = invoke.getMethodName(constantPoolGen)
        Type[] argumentTypes = invoke.getArgumentTypes(constantPoolGen)

        return new MethodDto([
                referencedClassName: referencedClassName,
                methodName: methodName,
                argumentTypes: argumentTypes
        ])
    }

    static class MethodDto {

        String referencedClassName
        String methodName
        Type[] argumentTypes
    }
}