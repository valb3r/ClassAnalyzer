package com.helpers.classrelationship.analysis.method.finegrained

import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.InvokeInstruction
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.ReferenceType
import org.apache.bcel.generic.Type

class ExternalCallAnalyzer {

    private final ClassFileAnalyzer classAnalyzer

    ExternalCallAnalyzer(ClassFileAnalyzer classAnalyzer) {
        this.classAnalyzer = classAnalyzer
    }

    ExternalMethodCallDto buildFromInstruction(InvokeInstruction invoke) {
        ConstantPoolGen constantPoolGen = classAnalyzer.internals().constPoolGen
        ReferenceType referenceType = invoke.getReferenceType(constantPoolGen)
        if (!(referenceType instanceof ObjectType)) {
            return null
        }

        ObjectType objectType = (ObjectType) referenceType
        String referencedClassName = objectType.getClassName()

        String methodName = invoke.getMethodName(constantPoolGen)
        Type[] argumentTypes = invoke.getArgumentTypes(constantPoolGen)

        return new ExternalMethodCallDto([
                referencedClassName: referencedClassName,
                methodName: methodName,
                argumentTypes: argumentTypes
        ])
    }

    static class ExternalMethodCallDto implements BodyAction {

        String referencedClassName
        String methodName
        Type[] argumentTypes
    }
}
