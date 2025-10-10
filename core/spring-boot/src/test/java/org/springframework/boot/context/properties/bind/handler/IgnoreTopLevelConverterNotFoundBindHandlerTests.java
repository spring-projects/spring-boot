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

package org.springframework.boot.context.properties.bind.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.core.convert.ConverterNotFoundException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link IgnoreTopLevelConverterNotFoundBindHandler}.
 *
 * @author Madhura Bhave
 */
class IgnoreTopLevelConverterNotFoundBindHandlerTests {

	private final List<ConfigurationPropertySource> sources = new ArrayList<>();

	private Binder binder;

	@BeforeEach
	void setup() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example", "bar");
		this.sources.add(source);
		this.binder = new Binder(this.sources);
	}

	@Test
	void bindWhenHandlerNotPresentShouldFail() {
		assertThatExceptionOfType(BindException.class)
			.isThrownBy(() -> this.binder.bind("example", Bindable.of(Example.class)))
			.withCauseInstanceOf(ConverterNotFoundException.class);
	}

	@Test
	void bindWhenTopLevelContextAndExceptionIgnorableShouldNotFail() {
		this.binder.bind("example", Bindable.of(Example.class), new IgnoreTopLevelConverterNotFoundBindHandler());
	}

	@Test
	void bindWhenExceptionNotIgnorableShouldFail() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.foo", "1");
		this.sources.add(source);
		assertThatExceptionOfType(BindException.class)
			.isThrownBy(() -> this.binder.bind("example", Bindable.of(Example.class),
					new IgnoreTopLevelConverterNotFoundBindHandler()))
			.withCauseInstanceOf(IllegalStateException.class);
	}

	@Test
	void bindWhenExceptionInNestedContextShouldFail() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.map", "hello");
		this.sources.add(source);
		assertThatExceptionOfType(BindException.class)
			.isThrownBy(() -> this.binder.bind("example", Bindable.of(Example.class),
					new IgnoreTopLevelConverterNotFoundBindHandler()))
			.withCauseInstanceOf(ConverterNotFoundException.class);
	}

	static class Example {

		private int foo;

		private @Nullable Map<String, String> map;

		int getFoo() {
			return this.foo;
		}

		void setFoo(int foo) {
			throw new IllegalStateException();
		}

		@Nullable Map<String, String> getMap() {
			return this.map;
		}

		void setMap(@Nullable Map<String, String> map) {
			this.map = map;
		}

	}

}
