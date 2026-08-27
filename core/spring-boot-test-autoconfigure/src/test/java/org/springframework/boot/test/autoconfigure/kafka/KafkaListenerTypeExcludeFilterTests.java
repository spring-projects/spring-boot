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

package org.springframework.boot.test.autoconfigure.kafka;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaListenerTypeExcludeFilter}.
 */
class KafkaListenerTypeExcludeFilterTests {

	private final MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Test
	void matchWhenHasNoListenersIncludesNothing() throws Exception {
		KafkaListenerTypeExcludeFilter filter = new KafkaListenerTypeExcludeFilter(NoListenersTest.class);
		assertThat(excludes(filter, ExampleListener.class)).isTrue();
		assertThat(excludes(filter, ExampleComponent.class)).isTrue();
	}

	@Test
	void matchWhenHasListenersIncludesOnlySpecifiedListeners() throws Exception {
		KafkaListenerTypeExcludeFilter filter = new KafkaListenerTypeExcludeFilter(WithListenersTest.class);
		assertThat(excludes(filter, ExampleListener.class)).isFalse();
		assertThat(excludes(filter, AnotherListener.class)).isTrue();
		assertThat(excludes(filter, ExampleComponent.class)).isTrue();
	}

	@Test
	void matchWhenHasMultipleListenersIncludesAllSpecified() throws Exception {
		KafkaListenerTypeExcludeFilter filter = new KafkaListenerTypeExcludeFilter(WithMultipleListenersTest.class);
		assertThat(excludes(filter, ExampleListener.class)).isFalse(); // Included!
		assertThat(excludes(filter, AnotherListener.class)).isFalse(); // Included!
		assertThat(excludes(filter, ExampleComponent.class)).isTrue(); // Excluded!
	}

	@Test
	void matchWhenHasValueAliasIncludesSpecifiedListeners() throws Exception {
		KafkaListenerTypeExcludeFilter filter = new KafkaListenerTypeExcludeFilter(WithValueTest.class);
		assertThat(excludes(filter, ExampleListener.class)).isFalse();
		assertThat(excludes(filter, AnotherListener.class)).isTrue();
	}

	@Test
	void matchWhenHasIncludeFilter() throws Exception {
		KafkaListenerTypeExcludeFilter filter = new KafkaListenerTypeExcludeFilter(WithIncludeFilterTest.class);
		assertThat(excludes(filter, ExampleListener.class)).isFalse();
		assertThat(excludes(filter, ExampleService.class)).isFalse();
		assertThat(excludes(filter, ExampleComponent.class)).isTrue();
	}

	@Test
	void matchWhenHasExcludeFilter() throws Exception {
		KafkaListenerTypeExcludeFilter filter = new KafkaListenerTypeExcludeFilter(WithExcludeFilterTest.class);
		assertThat(excludes(filter, ExampleListener.class)).isFalse();
		assertThat(excludes(filter, OverrideListener.class)).isTrue();
	}

	@Test
	void matchWhenUseDefaultFiltersIsFalse() throws Exception {
		KafkaListenerTypeExcludeFilter filter = new KafkaListenerTypeExcludeFilter(WithoutDefaultFiltersTest.class);
		assertThat(excludes(filter, ExampleListener.class)).isTrue();
	}

	private boolean excludes(KafkaListenerTypeExcludeFilter filter, Class<?> type) throws IOException {
		MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
		return filter.match(metadataReader, this.metadataReaderFactory);
	}

	@KafkaListenerTest
	static class NoListenersTest {

	}

	@KafkaListenerTest(listeners = ExampleListener.class)
	static class WithListenersTest {

	}

	@KafkaListenerTest(listeners = { ExampleListener.class, AnotherListener.class })
	static class WithMultipleListenersTest {

	}

	@KafkaListenerTest(ExampleListener.class)
	static class WithValueTest {

	}

	@KafkaListenerTest(listeners = ExampleListener.class,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ExampleService.class))
	static class WithIncludeFilterTest {

	}

	@KafkaListenerTest(listeners = { ExampleListener.class, OverrideListener.class },
			excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OverrideListener.class))
	static class WithExcludeFilterTest {

	}

	@KafkaListenerTest(listeners = ExampleListener.class, useDefaultFilters = false)
	static class WithoutDefaultFiltersTest {

	}

	static class ExampleListener {

	}

	static class AnotherListener {

	}

	static class OverrideListener {

	}

	@Service
	static class ExampleService {

	}

	@Component
	static class ExampleComponent {

	}

}
