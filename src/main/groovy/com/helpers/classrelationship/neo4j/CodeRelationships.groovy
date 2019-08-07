package com.helpers.classrelationship.neo4j

import org.neo4j.graphdb.RelationshipType

class CodeRelationships {
    enum Relationships implements RelationshipType {
        COMPOSED,
        PACKS,
        HAS,
        ARGUMENT,
        IS,
        IS_IN,
        RETURNS,
        CALLS,
        CALLS_LAMBDA,
        EXTENDS,
        IS_A,
        GET,
        PUT,
        USES_CLASS_NAME,
        OVERRIDES,
        OVERRIDDEN_BY
    }
}
