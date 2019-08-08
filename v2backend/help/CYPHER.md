# Cypher queries sample

# Precise insights
**Class analyzed - RateRule**

- **Call path which modifies class state**  
- **Call path which reads class state**
- **Call path which uses class but does not alter its state**

# Dirty and quick call insights:

**Random walk using neo4j graph algorithms library**
```genericsql
MATCH (m:Method {name: "readDocument", ownerSimpleName: "DocumentController"})
CALL algo.randomWalk.stream(id(m), 90, 350, {
  nodeQuery: "MATCH (p:Method) WHERE p.name <> '<init>' RETURN id(p) as id",
  relationshipQuery: "MATCH (p1:Method)-[:Calls|:OverriddenBy]->(p2:Method) RETURN id(p1) as source, id(p2) as target",
  mode: "node2vec",
  inOut: 1.0,
  return: 0.01,
  graph: "cypher"
})
YIELD nodeIds, path
UNWIND nodeIds AS nodeId
RETURN algo.asNode(nodeId) AS page

```

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
