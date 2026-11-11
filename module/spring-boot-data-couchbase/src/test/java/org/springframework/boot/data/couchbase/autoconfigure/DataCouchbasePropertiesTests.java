/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.data.couchbase.core.convert.DefaultCouchbaseTypeMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataCouchbaseProperties}.
 *
 * @author Stephane Nicoll
 */
class DataCouchbasePropertiesTests {

	@Test
	void typeKeyHasConsistentDefault() {
		assertThat(new DataCouchbaseProperties().getTypeKey()).isEqualTo(DefaultCouchbaseTypeMapper.DEFAULT_TYPE_KEY);
	}

}
