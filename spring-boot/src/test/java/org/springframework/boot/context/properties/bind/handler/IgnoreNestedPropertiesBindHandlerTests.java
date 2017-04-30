/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IgnoreNestedPropertiesBindHandler}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class IgnoreNestedPropertiesBindHandlerTests {

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private Binder binder;

	@Before
	public void setup() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.foo", "foovalue");
		source.put("example.nested.bar", "barvalue");
		this.sources.add(source);
		this.binder = new Binder(this.sources);
	}

	@Test
	public void bindWhenNotIngoringNestedShouldBindAll() throws Exception {
		Example bound = this.binder.bind("example", Bindable.of(Example.class)).get();
		assertThat(bound.getFoo()).isEqualTo("foovalue");
		assertThat(bound.getNested().getBar()).isEqualTo("barvalue");
	}

	@Test
	public void bindWhenIngoringNestedShouldFilterNested() throws Exception {
		Example bound = this.binder.bind("example", Bindable.of(Example.class),
				new IgnoreNestedPropertiesBindHandler()).get();
		assertThat(bound.getFoo()).isEqualTo("foovalue");
		assertThat(bound.getNested()).isNull();
	}

	public static class Example {

		private String foo;

		private ExampleNested nested;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public ExampleNested getNested() {
			return this.nested;
		}

		public void setNested(ExampleNested nested) {
			this.nested = nested;
		}

	}

	public static class ExampleNested {

		private String bar;

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

	}

}
