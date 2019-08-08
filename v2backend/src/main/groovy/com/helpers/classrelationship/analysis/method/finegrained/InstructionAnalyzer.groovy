package com.helpers.classrelationship.analysis.method.finegrained

import org.apache.bcel.generic.Instruction

interface InstructionAnalyzer<A extends InMethodBodyAction, I extends Instruction> {

    A analyze(I instruction)
}