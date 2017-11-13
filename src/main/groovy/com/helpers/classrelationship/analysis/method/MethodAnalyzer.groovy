package com.helpers.classrelationship.analysis.method

import com.google.common.collect.ImmutableMap
import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import com.helpers.classrelationship.analysis.MethodRegistry
import com.helpers.classrelationship.analysis.method.finegrained.InMethodBodyAction
import com.helpers.classrelationship.analysis.method.finegrained.ExternalCallAnalyzer
import com.helpers.classrelationship.analysis.method.finegrained.FieldCallAnalyzer
import com.helpers.classrelationship.analysis.method.finegrained.InstructionAnalyzer
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.FieldInstruction
import org.apache.bcel.generic.Instruction
import org.apache.bcel.generic.InstructionList
import org.apache.bcel.generic.InvokeInstruction
import org.apache.bcel.generic.MethodGen

class MethodAnalyzer {

    private final MethodRegistry methodRegistry
    private final ClassFileAnalyzer classAnalyzer;
    private final Method method;

    private final Map<Class, InstructionAnalyzer> dispatchers = ImmutableMap.builder()
            .put(InvokeInstruction.class, new ExternalCallAnalyzer(classAnalyzer))
            .put(FieldInstruction.class, new FieldCallAnalyzer(classAnalyzer))
            .build()

    MethodAnalyzer(MethodRegistry methodRegistry, ClassFileAnalyzer classAnalyzer, Method method) {
        this.methodRegistry = methodRegistry
        this.classAnalyzer = classAnalyzer
        this.method = method
    }

    List<InMethodBodyAction> analyze() {
        String fullClassName = classAnalyzer.get().getClassName()
        MethodGen mg = new MethodGen(method, fullClassName, classAnalyzer.internals().constPoolGen)
        InstructionList il = mg.getInstructionList()
        if (il == null) {
            return Collections.emptyList()
        }

        // limit to external class calls
        return il.getInstructionHandles().toList().stream()
                .map {it.getInstruction()}
                .map {getDispatcher(it)?.analyze(it)}
                .filter {null != it}
                .collect {it}
    }

    private InstructionAnalyzer getDispatcher(Instruction instruction) {
        dispatchers.get(
                dispatchers.keySet().find {kind -> kind.isInstance(instruction)}
        )
    }
}