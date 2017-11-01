package com.helpers.classrelationship

class Benchmark {

    static def method = { name, info, closure ->
        def start = System.currentTimeMillis()
        println "Executing $name ($info)"
        closure.call()
        def now = System.currentTimeMillis()
        println "Execution of $name took: ${(now - start) / 1000.0} s"
    }
}
