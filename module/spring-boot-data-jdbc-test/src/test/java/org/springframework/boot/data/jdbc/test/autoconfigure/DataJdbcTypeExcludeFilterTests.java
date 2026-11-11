/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jdbc.test.autoconfigure;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataJdbcTypeExcludeFilter}.
 *
 * @author Ravi Undupitiya
 * @author Stephane Nicoll
 */
class DataJdbcTypeExcludeFilterTests {

	private final MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Test
	void matchUsingDefaultFilters() throws Exception {
		DataJdbcTypeExcludeFilter filter = new DataJdbcTypeExcludeFilter(UsingDefaultFilters.class);
		assertThat(excludes(filter, TestJdbcConfiguration.class)).isFalse();
	}

	@Test
	void matchNotUsingDefaultFilters() throws Exception {
		DataJdbcTypeExcludeFilter filter = new DataJdbcTypeExcludeFilter(NotUsingDefaultFilters.class);
		assertThat(excludes(filter, TestJdbcConfiguration.class)).isTrue();
	}

	private boolean excludes(DataJdbcTypeExcludeFilter filter, Class<?> type) throws IOException {
		MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
		return filter.match(metadataReader, this.metadataReaderFactory);
	}

	@DataJdbcTest
	static class UsingDefaultFilters {

	}

	@DataJdbcTest(useDefaultFilters = false)
	static class NotUsingDefaultFilters {

	}

	static class TestJdbcConfiguration extends AbstractJdbcConfiguration {

	}

}
