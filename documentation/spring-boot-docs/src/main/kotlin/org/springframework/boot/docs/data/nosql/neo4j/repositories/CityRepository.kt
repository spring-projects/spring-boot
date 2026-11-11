/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.neo4j.repositories

import org.springframework.data.neo4j.repository.Neo4jRepository

interface CityRepository : Neo4jRepository<City, Long> {

	fun findOneByNameAndState(name: String?, state: String?): City?

}

