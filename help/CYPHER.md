# Cypher queries sample

**Virtual class level relationship tree**

    MATCH p = (s:Class)-[:Has]->(m:Method)-[:Calls*7..7]->(em:Method)<-[:Has]-(e:Class) WHERE e.simpleName = 'RateRule' AND s <> e 
    WITH EXTRACT(r IN RELATIONSHIPS(p)| ID(r)) AS relIds
    MATCH (o:Class)-[:Has]->(mo:Method)-[r]->(md:Method)<-[:Has]-(d:Class) WHERE ID(r) IN relIds
    WITH DISTINCT o, d
    CALL apoc.create.vRelationship(o,'Calls',{}, d) YIELD rel
    RETURN o, d, rel


**Class-method call chain relationship (Start/End class, method calls in between):**

    MATCH p = (s:Class)-[:Has]->(m:Method)-[:Calls*7..7]->(em:Method)<-[:Has]-(e:Class) WHERE e.simpleName = 'RateRule' AND s <> e 
    WITH COLLECT(p) AS nodes
    RETURN nodes