/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.context.properties;

/**
 * @author pwebb
 */

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.support.TestPropertySourceUtils;

public class TempTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	void testName() {
		load(MyConfig.class, "foo.bar.baz=hello");
		System.out.println(this.context.getBean(Foo.class));
	}

	@Test
	void testName2() {
		load(MyConfig.class);
		System.out.println(this.context.getBean(Foo.class));
	}

	private AnnotationConfigApplicationContext load(Class<?> configuration, String... inlinedProperties) {
		return load(new Class<?>[] { configuration }, inlinedProperties);
	}

	private AnnotationConfigApplicationContext load(Class<?>[] configuration, String... inlinedProperties) {
		this.context.register(configuration);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, inlinedProperties);
		this.context.refresh();
		return this.context;
	}

	@Configuration
	@EnableConfigurationProperties(Foo.class)
	static class MyConfig {

	}

	@ConfigurationProperties("foo")
	record Foo(@DefaultValue Bar bar) {
	}

	record Bar(@DefaultValue("hello") String baz) {
	}

}
