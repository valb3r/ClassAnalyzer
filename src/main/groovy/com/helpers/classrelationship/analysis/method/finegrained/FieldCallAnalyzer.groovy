package com.helpers.classrelationship.analysis.method.finegrained

import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import org.apache.bcel.generic.FieldInstruction
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

class FieldCallAnalyzer extends FieldOrMethodAnalyzer<FieldChangeDto, FieldInstruction> {

    FieldCallAnalyzer(ClassFileAnalyzer classAnalyzer) {
        super(classAnalyzer)
    }

    @Override
    FieldChangeDto doAnalyze(FieldInstruction invoke, ReferenceType referenceType) {

        String fieldName = invoke.getFieldName(constantPoolGen)
        Type fieldType = invoke.getFieldType(constantPoolGen)

        return new FieldChangeDto([
                fieldName: fieldName,
                fieldType: fieldType
        ])
    }

    static class FieldChangeDto implements InMethodBodyAction {

        String fieldName
        Type fieldType
    }
}
