package com.helpers.classrelationship.analysis.method.finegrained

import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.LDC
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.Type

class ClassNameReferenceAnalyzer implements InstructionAnalyzer<ClassNameReferenceDto, LDC> {

    private final ClassFileAnalyzer classAnalyzer

    ClassNameReferenceAnalyzer(ClassFileAnalyzer classAnalyzer) {
        this.classAnalyzer = classAnalyzer
    }

    @Override
    ClassNameReferenceDto analyze(LDC instruction) {
        ConstantPoolGen constantPoolGen = classAnalyzer.internals().constPoolGen

        if (Type.CLASS != instruction.getType(constantPoolGen)) {
            return null
        }

        Object baseValue = instruction.getValue(constantPoolGen)
        if (!(baseValue instanceof ObjectType)) {
            return null
        }

        ObjectType type = (ObjectType) baseValue

        return new ClassNameReferenceDto([
                referencedClassName: type.getClassName()
        ])
    }

    static class ClassNameReferenceDto implements InMethodBodyAction {

        String referencedClassName
    }
}
