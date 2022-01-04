/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.data.jdbc;

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
