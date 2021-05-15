/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link BoundPropertiesTrackingBindHandler}.
 *
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
public class BoundPropertiesTrackingBindHandlerTests {

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private BoundPropertiesTrackingBindHandler handler;

	private Binder binder;

	@Mock
	private Consumer<ConfigurationProperty> consumer;

	@BeforeEach
	void setup() {
		this.binder = new Binder(this.sources);
		this.handler = new BoundPropertiesTrackingBindHandler(this.consumer);
	}

	@Test
	void handlerShouldCallRecordBindingIfConfigurationPropertyIsNotNull() {
		this.sources.add(new MockConfigurationPropertySource("foo.age", 4));
		this.binder.bind("foo", Bindable.of(ExampleBean.class), this.handler);
		verify(this.consumer, times(1)).accept(any(ConfigurationProperty.class));
		verify(this.consumer, never()).accept(null);
	}

	static class ExampleBean {

		private int age;

		int getAge() {
			return this.age;
		}

		void setAge(int age) {
			this.age = age;
		}

	}

}
