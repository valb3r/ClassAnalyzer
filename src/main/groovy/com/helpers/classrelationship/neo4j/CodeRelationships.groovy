package com.helpers.classrelationship.neo4j

import org.neo4j.graphdb.RelationshipType

class CodeRelationships {
    enum Relationships implements RelationshipType {
        Composed,
        Packs,
        Has,
        Argument,
        Is,
        IsIn,
        Returns,
        Calls,
        Extends,
        IsA,
        Get,
        Put,
        UsesClassName,
        Overrides,
        OverriddenBy
    }
}
