/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.domain.scan;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;

public class TestNonAnnotated {

	@Id
	@GeneratedValue
	@SuppressWarnings("NullAway.Init")
	private Long id;

}
