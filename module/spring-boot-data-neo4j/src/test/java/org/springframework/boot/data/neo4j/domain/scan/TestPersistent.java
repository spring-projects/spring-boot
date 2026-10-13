/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.domain.scan;

import org.springframework.data.annotation.Persistent;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;

@Persistent
public class TestPersistent {

	@Id
	@GeneratedValue
	@SuppressWarnings("NullAway.Init")
	private Long id;

}
