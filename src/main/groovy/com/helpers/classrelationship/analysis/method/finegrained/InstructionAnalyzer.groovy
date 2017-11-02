package com.helpers.classrelationship.analysis.method.finegrained

import org.apache.bcel.generic.Instruction

interface InstructionAnalyzer {

    BodyAction analyze(Instruction instruction)
}