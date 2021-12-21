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

package org.springframework.boot.test.autoconfigure.graphql;

import java.io.IOException;

import com.fasterxml.jackson.databind.module.SimpleModule;
import graphql.schema.idl.RuntimeWiring;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.web.WebInput;
import org.springframework.graphql.web.WebInterceptor;
import org.springframework.graphql.web.WebInterceptorChain;
import org.springframework.graphql.web.WebOutput;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphQlTypeExcludeFilter}
 *
 * @author Brian Clozel
 */
class GraphQlTypeExcludeFilterTests {

	private MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Test
	void matchWhenHasNoControllers() throws Exception {
		GraphQlTypeExcludeFilter filter = new GraphQlTypeExcludeFilter(WithNoControllers.class);
		assertThat(excludes(filter, Controller1.class)).isFalse();
		assertThat(excludes(filter, Controller2.class)).isFalse();
		assertThat(excludes(filter, ExampleRuntimeWiringConfigurer.class)).isFalse();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isTrue();
		assertThat(excludes(filter, ExampleWebInterceptor.class)).isTrue();
		assertThat(excludes(filter, ExampleModule.class)).isFalse();
	}

	@Test
	void matchWhenHasController() throws Exception {
		GraphQlTypeExcludeFilter filter = new GraphQlTypeExcludeFilter(WithController.class);
		assertThat(excludes(filter, Controller1.class)).isFalse();
		assertThat(excludes(filter, Controller2.class)).isTrue();
		assertThat(excludes(filter, ExampleRuntimeWiringConfigurer.class)).isFalse();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isTrue();
		assertThat(excludes(filter, ExampleWebInterceptor.class)).isTrue();
		assertThat(excludes(filter, ExampleModule.class)).isFalse();
	}

	@Test
	void matchNotUsingDefaultFilters() throws Exception {
		GraphQlTypeExcludeFilter filter = new GraphQlTypeExcludeFilter(NotUsingDefaultFilters.class);
		assertThat(excludes(filter, Controller1.class)).isTrue();
		assertThat(excludes(filter, Controller2.class)).isTrue();
		assertThat(excludes(filter, ExampleRuntimeWiringConfigurer.class)).isTrue();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isTrue();
		assertThat(excludes(filter, ExampleWebInterceptor.class)).isTrue();
		assertThat(excludes(filter, ExampleModule.class)).isTrue();
	}

	@Test
	void matchWithIncludeFilter() throws Exception {
		GraphQlTypeExcludeFilter filter = new GraphQlTypeExcludeFilter(WithIncludeFilter.class);
		assertThat(excludes(filter, Controller1.class)).isFalse();
		assertThat(excludes(filter, Controller2.class)).isFalse();
		assertThat(excludes(filter, ExampleRuntimeWiringConfigurer.class)).isFalse();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isFalse();
		assertThat(excludes(filter, ExampleWebInterceptor.class)).isTrue();
		assertThat(excludes(filter, ExampleModule.class)).isFalse();
	}

	@Test
	void matchWithExcludeFilter() throws Exception {
		GraphQlTypeExcludeFilter filter = new GraphQlTypeExcludeFilter(WithExcludeFilter.class);
		assertThat(excludes(filter, Controller1.class)).isTrue();
		assertThat(excludes(filter, Controller2.class)).isFalse();
		assertThat(excludes(filter, ExampleRuntimeWiringConfigurer.class)).isFalse();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isTrue();
		assertThat(excludes(filter, ExampleWebInterceptor.class)).isTrue();
		assertThat(excludes(filter, ExampleModule.class)).isFalse();
	}

	private boolean excludes(GraphQlTypeExcludeFilter filter, Class<?> type) throws IOException {
		MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
		return filter.match(metadataReader, this.metadataReaderFactory);
	}

	@GraphQlTest
	static class WithNoControllers {

	}

	@GraphQlTest(Controller1.class)
	static class WithController {

	}

	@GraphQlTest(useDefaultFilters = false)
	static class NotUsingDefaultFilters {

	}

	@GraphQlTest(includeFilters = @ComponentScan.Filter(Repository.class))
	static class WithIncludeFilter {

	}

	@GraphQlTest(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Controller1.class))
	static class WithExcludeFilter {

	}

	@Controller
	static class Controller1 {

	}

	@Controller
	static class Controller2 {

	}

	@Service
	static class ExampleService {

	}

	@Repository
	static class ExampleRepository {

	}

	static class ExampleRuntimeWiringConfigurer implements RuntimeWiringConfigurer {

		@Override
		public void configure(RuntimeWiring.Builder builder) {

		}

	}

	static class ExampleWebInterceptor implements WebInterceptor {

		@Override
		public Mono<WebOutput> intercept(WebInput webInput, WebInterceptorChain chain) {
			return null;
		}

	}

	@SuppressWarnings("serial")
	static class ExampleModule extends SimpleModule {

	}

}
