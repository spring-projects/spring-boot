/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.webservices.server;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.ws.server.endpoint.annotation.Endpoint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServiceServerTypeExcludeFilter}.
 *
 * @author Daniil Razorenov
 */
class WebServiceServerTypeExcludeFilterTests {

	private final MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Test
	void matchWhenHasNoEndpoints() throws IOException {
		WebServiceServerTypeExcludeFilter filter = new WebServiceServerTypeExcludeFilter(WithNoEndpoints.class);
		assertThat(exclude(filter, WebService1.class)).isFalse();
		assertThat(exclude(filter, WebService2.class)).isFalse();
		assertThat(exclude(filter, ExampleService.class)).isTrue();
		assertThat(exclude(filter, ExampleRepository.class)).isTrue();
	}

	@Test
	void matchWhenHasEndpoint() throws IOException {
		WebServiceServerTypeExcludeFilter filter = new WebServiceServerTypeExcludeFilter(WithEndpoint.class);
		assertThat(exclude(filter, WebService1.class)).isFalse();
		assertThat(exclude(filter, WebService2.class)).isTrue();
		assertThat(exclude(filter, ExampleService.class)).isTrue();
		assertThat(exclude(filter, ExampleRepository.class)).isTrue();
	}

	@Test
	void matchNotUsingDefaultFilters() throws IOException {
		WebServiceServerTypeExcludeFilter filter = new WebServiceServerTypeExcludeFilter(NotUsingDefaultFilters.class);
		assertThat(exclude(filter, WebService1.class)).isTrue();
		assertThat(exclude(filter, WebService2.class)).isTrue();
		assertThat(exclude(filter, ExampleService.class)).isTrue();
		assertThat(exclude(filter, ExampleRepository.class)).isTrue();
	}

	@Test
	void matchWithIncludeFilter() throws IOException {
		WebServiceServerTypeExcludeFilter filter = new WebServiceServerTypeExcludeFilter(WithIncludeFilter.class);
		assertThat(exclude(filter, WebService1.class)).isFalse();
		assertThat(exclude(filter, WebService2.class)).isFalse();
		assertThat(exclude(filter, ExampleService.class)).isTrue();
		assertThat(exclude(filter, ExampleRepository.class)).isFalse();
	}

	@Test
	void matchWithExcludeFilter() throws IOException {
		WebServiceServerTypeExcludeFilter filter = new WebServiceServerTypeExcludeFilter(WithExcludeFilter.class);
		assertThat(exclude(filter, WebService1.class)).isTrue();
		assertThat(exclude(filter, WebService2.class)).isFalse();
		assertThat(exclude(filter, ExampleService.class)).isTrue();
		assertThat(exclude(filter, ExampleRepository.class)).isTrue();
	}

	private boolean exclude(WebServiceServerTypeExcludeFilter filter, Class<?> type) throws IOException {
		MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
		return filter.match(metadataReader, this.metadataReaderFactory);
	}

	@WebServiceServerTest
	static class WithNoEndpoints {

	}

	@WebServiceServerTest(WebService1.class)
	static class WithEndpoint {

	}

	@WebServiceServerTest(useDefaultFilters = false)
	static class NotUsingDefaultFilters {

	}

	@WebServiceServerTest(includeFilters = @Filter(Repository.class))
	static class WithIncludeFilter {

	}

	@WebServiceServerTest(excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebService1.class))
	static class WithExcludeFilter {

	}

	@Endpoint
	static class WebService1 {

	}

	@Endpoint
	static class WebService2 {

	}

	@Service
	static class ExampleService {

	}

	@Repository
	static class ExampleRepository {

	}

}
