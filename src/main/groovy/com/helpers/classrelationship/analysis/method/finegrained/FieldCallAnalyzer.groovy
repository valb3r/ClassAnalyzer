package com.helpers.classrelationship.analysis.method.finegrained

import com.google.common.collect.ImmutableMap
import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import org.apache.bcel.generic.FieldInstruction
import org.apache.bcel.generic.GETFIELD
import org.apache.bcel.generic.GETSTATIC
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.PUTFIELD
import org.apache.bcel.generic.PUTSTATIC
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

class FieldCallAnalyzer extends FieldOrMethodAnalyzer<FieldCallDto, FieldInstruction> {

    FieldCallAnalyzer(ClassFileAnalyzer classAnalyzer) {
        super(classAnalyzer)
    }

    @Override
    FieldCallDto doAnalyze(FieldInstruction invoke, ReferenceType referenceType) {

        // Currently don't see need to analyze arrays
        if (!invoke.getReferenceType(constantPoolGen) instanceof ObjectType) {
            return null
        }

        ObjectType ref = (ObjectType) invoke.getReferenceType(constantPoolGen)

        String fieldName = invoke.getFieldName(constantPoolGen)
        Type fieldType = invoke.getFieldType(constantPoolGen)

        String referencedClassName = ref.getClassName()

        return new FieldCallDto([
                referencedClassName: referencedClassName,
                fieldName: fieldName,
                fieldType: fieldType,
                kind: FieldCallDto.KIND.get(invoke.getClass())
        ])
    }

    static class FieldCallDto implements InMethodBodyAction {

        static enum Kind {PUT, GET}

        static final Map<Class, Kind> KIND = ImmutableMap.<Class, Kind>of(
                GETFIELD.class, Kind.GET,
                GETSTATIC.class, Kind.GET,
                PUTFIELD.class, Kind.PUT,
                PUTSTATIC.class, Kind.PUT
        )

        String referencedClassName
        String fieldName
        Type fieldType
        Kind kind
    }
}
