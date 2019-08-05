package com.helpers.classrelationship.neo4j.persistor

class Constants {

    class App {

        static final String NAME = "name"
    }

    class Jar {

        static final String NAME = "name"
        static final String PATHS = "paths"
    }

    class Class {

        static final String NAME = "name"
        static final String SIMPLE_NAME = "simpleName"
        static final String PACKAGE = "packageName"
        static final String CLASS = "class"
        static final String INTERFACE = "interface"

        static final String JAR = "jar"
        static final String HASH = "hash"
        static final String APP = "appName"
    }

    class Field {

        static final String NAME = "name"
        static final String TYPE = "type"
        static final String OWNER_SIMPLE_NAME = "ownerSimpleName"
        static final String CALL_COUNT = "count"
    }

    class Method {

        static final String NAME = "name"
        static final String RETURN = "return"
        static final String ARGS = "args"
        static final String OWNER_SIMPLE_NAME = "ownerSimpleName"
        static final String CALL_COUNT = "count"
        static final String MIN_POSITION = "minPos"
        static final String POSITION = "position"

        class Arg {

            static final String NAME = "name"
            static final String TYPE = "type"
        }
    }

    class StaticReference {

        static final String CALL_COUNT = "count"
    }
}
