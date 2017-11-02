package com.helpers.classrelationship.analysis.method.finegrained

import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.FieldInstruction
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

class FieldCallAnalyzer implements InstructionAnalyzer {

    private final ClassFileAnalyzer classAnalyzer

    FieldCallAnalyzer(ClassFileAnalyzer classAnalyzer) {
        this.classAnalyzer = classAnalyzer
    }

    FieldChangeDto buildFromInstruction(FieldInstruction invoke) {
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

    static class FieldChangeDto implements BodyAction {

        String referencedClassName
        String methodName
        Type[] argumentTypes
    }
}
