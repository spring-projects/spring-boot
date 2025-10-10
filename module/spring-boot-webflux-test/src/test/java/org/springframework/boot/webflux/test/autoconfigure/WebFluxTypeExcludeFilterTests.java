/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webflux.test.autoconfigure;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import tools.jackson.databind.module.SimpleModule;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxTypeExcludeFilter}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class WebFluxTypeExcludeFilterTests {

	private final MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Test
	void matchWhenHasNoControllers() throws Exception {
		WebFluxTypeExcludeFilter filter = new WebFluxTypeExcludeFilter(WithNoControllers.class);
		assertThat(excludes(filter, Controller1.class)).isFalse();
		assertThat(excludes(filter, Controller2.class)).isFalse();
		assertThat(excludes(filter, ExampleControllerAdvice.class)).isFalse();
		assertThat(excludes(filter, ExampleWeb.class)).isFalse();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isTrue();
		assertThat(excludes(filter, ExampleWebFilter.class)).isFalse();
		assertThat(excludes(filter, ExampleModule.class)).isFalse();
	}

	@Test
	void matchWhenHasController() throws Exception {
		WebFluxTypeExcludeFilter filter = new WebFluxTypeExcludeFilter(WithController.class);
		assertThat(excludes(filter, Controller1.class)).isFalse();
		assertThat(excludes(filter, Controller2.class)).isTrue();
		assertThat(excludes(filter, ExampleControllerAdvice.class)).isFalse();
		assertThat(excludes(filter, ExampleWeb.class)).isFalse();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isTrue();
		assertThat(excludes(filter, ExampleWebFilter.class)).isFalse();
		assertThat(excludes(filter, ExampleModule.class)).isFalse();
	}

	@Test
	void matchNotUsingDefaultFilters() throws Exception {
		WebFluxTypeExcludeFilter filter = new WebFluxTypeExcludeFilter(NotUsingDefaultFilters.class);
		assertThat(excludes(filter, Controller1.class)).isTrue();
		assertThat(excludes(filter, Controller2.class)).isTrue();
		assertThat(excludes(filter, ExampleControllerAdvice.class)).isTrue();
		assertThat(excludes(filter, ExampleWeb.class)).isTrue();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isTrue();
		assertThat(excludes(filter, ExampleWebFilter.class)).isTrue();
		assertThat(excludes(filter, ExampleModule.class)).isTrue();
	}

	@Test
	void matchWithIncludeFilter() throws Exception {
		WebFluxTypeExcludeFilter filter = new WebFluxTypeExcludeFilter(WithIncludeFilter.class);
		assertThat(excludes(filter, Controller1.class)).isFalse();
		assertThat(excludes(filter, Controller2.class)).isFalse();
		assertThat(excludes(filter, ExampleControllerAdvice.class)).isFalse();
		assertThat(excludes(filter, ExampleWeb.class)).isFalse();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isFalse();
		assertThat(excludes(filter, ExampleWebFilter.class)).isFalse();
		assertThat(excludes(filter, ExampleModule.class)).isFalse();
	}

	@Test
	void matchWithExcludeFilter() throws Exception {
		WebFluxTypeExcludeFilter filter = new WebFluxTypeExcludeFilter(WithExcludeFilter.class);
		assertThat(excludes(filter, Controller1.class)).isTrue();
		assertThat(excludes(filter, Controller2.class)).isFalse();
		assertThat(excludes(filter, ExampleControllerAdvice.class)).isFalse();
		assertThat(excludes(filter, ExampleWeb.class)).isFalse();
		assertThat(excludes(filter, ExampleService.class)).isTrue();
		assertThat(excludes(filter, ExampleRepository.class)).isTrue();
		assertThat(excludes(filter, ExampleWebFilter.class)).isFalse();
		assertThat(excludes(filter, ExampleModule.class)).isFalse();
	}

	private boolean excludes(WebFluxTypeExcludeFilter filter, Class<?> type) throws IOException {
		MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
		return filter.match(metadataReader, this.metadataReaderFactory);
	}

	@WebFluxTest
	static class WithNoControllers {

	}

	@WebFluxTest(Controller1.class)
	static class WithController {

	}

	@WebFluxTest(useDefaultFilters = false)
	static class NotUsingDefaultFilters {

	}

	@WebFluxTest(includeFilters = @Filter(Repository.class))
	static class WithIncludeFilter {

	}

	@WebFluxTest(excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Controller1.class))
	static class WithExcludeFilter {

	}

	@Controller
	static class Controller1 {

	}

	@Controller
	static class Controller2 {

	}

	@ControllerAdvice
	static class ExampleControllerAdvice {

	}

	static class ExampleWeb implements WebFluxConfigurer {

	}

	@Service
	static class ExampleService {

	}

	@Repository
	static class ExampleRepository {

	}

	static class ExampleWebFilter implements WebFilter {

		@Override
		public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
			return null;
		}

	}

	static class ExampleModule extends SimpleModule {

	}

}
