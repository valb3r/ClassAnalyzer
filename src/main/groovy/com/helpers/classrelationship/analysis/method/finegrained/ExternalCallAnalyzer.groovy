package com.helpers.classrelationship.analysis.method.finegrained

import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.InvokeInstruction
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.ReferenceType
import org.apache.bcel.generic.Type

class ExternalCallAnalyzer extends FieldOrMethodAnalyzer<ExternalMethodCallDto, InvokeInstruction> {

    ExternalCallAnalyzer(ClassFileAnalyzer classAnalyzer) {
        super(classAnalyzer)
    }

    @Override
    ExternalMethodCallDto doAnalyze(InvokeInstruction invoke, ReferenceType referenceType) {
        ConstantPoolGen constantPoolGen = classAnalyzer.internals().constPoolGen

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

    static class ExternalMethodCallDto implements InMethodBodyAction {

        String referencedClassName
        String methodName
        Type[] argumentTypes
    }
}
