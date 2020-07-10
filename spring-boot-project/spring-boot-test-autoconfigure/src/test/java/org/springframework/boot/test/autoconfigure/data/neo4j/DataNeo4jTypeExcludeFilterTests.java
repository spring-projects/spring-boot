/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.test.autoconfigure.data.neo4j;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class DataNeo4jTypeExcludeFilterTests {

	private MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Test
	void matchWithExcludeFilter() throws Exception {
		DataNeo4jTypeExcludeFilter filter = new DataNeo4jTypeExcludeFilter(WithExcludeFilter.class);
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isTrue();
	}

	@Test
	void matchWithoutExcludeFilter() throws Exception {
		DataNeo4jTypeExcludeFilter filter = new DataNeo4jTypeExcludeFilter(WithoutExcludeFilter.class);
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isFalse();
	}

	@DataNeo4jTest(
			excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ExampleRepository.class))
	static class WithExcludeFilter {

	}

	@DataNeo4jTest
	static class WithoutExcludeFilter {

	}

	private boolean excludes(DataNeo4jTypeExcludeFilter filter, Class<?> type) throws IOException {
		MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
		return filter.match(metadataReader, this.metadataReaderFactory);
	}

}
