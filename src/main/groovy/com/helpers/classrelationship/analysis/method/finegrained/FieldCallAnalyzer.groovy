package com.helpers.classrelationship.analysis.method.finegrained

import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import org.apache.bcel.generic.FieldInstruction
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

class FieldCallAnalyzer extends FieldOrMethodAnalyzer<FieldCallDto, FieldInstruction> {

    FieldCallAnalyzer(ClassFileAnalyzer classAnalyzer) {
        super(classAnalyzer)
    }

    @Override
    FieldCallDto doAnalyze(FieldInstruction invoke, ReferenceType referenceType) {

        String fieldName = invoke.getFieldName(constantPoolGen)
        Type fieldType = invoke.getFieldType(constantPoolGen)

        return new FieldCallDto([
                fieldName: fieldName,
                fieldType: fieldType
        ])
    }

    static class FieldCallDto implements InMethodBodyAction {

        String fieldName
        Type fieldType
    }
}
