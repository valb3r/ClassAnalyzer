package com.helpers.classrelationship.analysis.method.finegrained

import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.FieldOrMethod
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.ReferenceType

abstract class FieldOrMethodAnalyzer<A extends InMethodBodyAction, I extends FieldOrMethod> implements InstructionAnalyzer<A, I> {

    final ClassFileAnalyzer classAnalyzer
    final ConstantPoolGen constantPoolGen

    FieldOrMethodAnalyzer(ClassFileAnalyzer classAnalyzer) {
        this.classAnalyzer = classAnalyzer
        this.constantPoolGen = classAnalyzer.internals().constPoolGen
    }

    @Override
    A analyze(I instruction) {
        ReferenceType referenceType = filterApplicability(instruction)
        return referenceType ? doAnalyze(instruction, referenceType) : null
    }

    ReferenceType filterApplicability(I instruction) {
        ReferenceType referenceType = instruction.getReferenceType(constantPoolGen)
        if (!(referenceType instanceof ObjectType)) {
            return null
        }

        return referenceType
    }

    abstract A doAnalyze(I instruction, ReferenceType referenceType)
}
