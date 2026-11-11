/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.domain.city;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface CityRepository extends Neo4jRepository<City, Long> {

	@Override
	Page<City> findAll(Pageable pageable);

}
