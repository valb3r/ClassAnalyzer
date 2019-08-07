package com.helpers.classrelationship.analysis.method.finegrained

import com.helpers.classrelationship.analysis.ClassFileAnalyzer
import org.apache.bcel.classfile.BootstrapMethods
import org.apache.bcel.classfile.ConstantInvokeDynamic
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.INVOKEDYNAMIC
import org.apache.bcel.generic.ReferenceType
import org.apache.bcel.generic.Type
/**
 * Analyzing lambdas requires at minimum call stack and local store/read imitation.
 * Lambda `instantiation` can be detected by invokedynamic instruction that links to special constant pool entry
 * this is typically sent to local variable or is found on stack and called using invokeinterface.
 */
class LambdaCallAnalyzer extends FieldOrMethodAnalyzer<LambdaCallDto, INVOKEDYNAMIC> {

    LambdaCallAnalyzer(ClassFileAnalyzer classAnalyzer) {
        super(classAnalyzer)
    }

    @Override
    LambdaCallDto doAnalyze(INVOKEDYNAMIC invoke, ReferenceType referenceType) {
        detectLambdaInstantiation(invoke, referenceType)
        return null
    }

    private LambdaCallDto detectLambdaInstantiation(INVOKEDYNAMIC invoke, ReferenceType referenceType) {
        def lambdaCallId = constantPoolGen.getConstant(invoke.getIndex())

        if (!(lambdaCallId instanceof ConstantInvokeDynamic)) {
            return null
        }

        def bootstrap = classAnalyzer.get().getAttributes().find {it -> it instanceof BootstrapMethods}

        if (!bootstrap) {
            return null
        }

        /*
            Possibly better solution:
            def method = ((BootstrapMethods) bootstrap).getBootstrapMethods()[lambdaCallId.getBootstrapMethodAttrIndex()]
        */
        // hacketty-hack - lambdaCallId is in synthetic method signature
        // i.e. private static synthetic String lambda$getBookings$0(String str) - 0 == lambdaCallId
        Method method = classAnalyzer.get().methods.toList()
                .stream()
                .filter {it.isSynthetic()}
                .filter {it.name.contains(' lambda$')}
                .filter {it.name.endsWith(String.valueOf(lambdaCallId.getBootstrapMethodAttrIndex()))}
                .findFirst().orElse(null)

        if (!method) {
            return null
        }

        return new LambdaCallDto([
                referencedClassName: classAnalyzer.get().className,
                methodName: method.getName(),
                argumentTypes: method.getArgumentTypes()
        ])
    }

    static class LambdaCallDto implements InMethodBodyAction {

        String referencedClassName
        String methodName
        Type[] argumentTypes
    }
}
