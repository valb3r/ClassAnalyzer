package com.helpers.classrelationship.neo4j

import org.neo4j.unsafe.batchinsert.BatchInserter
import org.neo4j.unsafe.batchinsert.BatchInserters

class Neo4j {

    private static final CONFIG = [
            "use_memory_mapped_buffers": "true",
            "neostore.nodestore.db.mapped_memory": "250M",
            "neostore.relationshipstore.db.mapped_memory": "1G",
            "neostore.propertystore.db.mapped_memory": "500M",
            "neostore.propertystore.db.strings.mapped_memory": "500M",
            "neostore.propertystore.db.arrays.mapped_memory": "0M",
            "cache_type": "none",
            "dump_config": "true"
    ]

    private final BatchInserter inserter

    Neo4j(File store) {
        this.inserter = BatchInserters.inserter(store, CONFIG)
    }

    BatchInserter getInserter() {
        return inserter
    }
}

