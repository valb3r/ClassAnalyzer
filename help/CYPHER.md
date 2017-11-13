# Cypher queries sample

# Precise insights
**Class analyzed - RateRule**

- **Call path which modifies class state**  
- **Call path which reads class state**
- **Call path which uses class but does not alter its state**

# Dirty and quick call insights:

**Class analyzed - RateRule**

**A-C class level path. Class A can possibly modify state of class C by calling B (virtual class level relationship tree)**

    MATCH p = (s:Class)-[:Has]->(m:Method)-[:Calls*7..7]->(em:Method)<-[:Has]-(e:Class) WHERE e.simpleName = 'RateRule' AND s <> e 
    WITH EXTRACT(r IN RELATIONSHIPS(p)| ID(r)) AS relIds
    MATCH (o:Class)-[:Has]->(mo:Method)-[r]->(md:Method)<-[:Has]-(d:Class) WHERE ID(r) IN relIds
    WITH DISTINCT o, d
    CALL apoc.create.vRelationship(o,'Calls',{}, d) YIELD rel
    RETURN o, d, rel

**A-C method level path. Class A can possibly modify state of class C through A.call() -> B.callD() -> C.callMe() class-method call chain relationship (Start/End class, method calls in between):**

    MATCH p = (s:Class)-[:Has]->(m:Method)-[:Calls*7..7]->(em:Method)<-[:Has]-(e:Class) WHERE e.simpleName = 'RateRule' AND s <> e 
    RETURN p