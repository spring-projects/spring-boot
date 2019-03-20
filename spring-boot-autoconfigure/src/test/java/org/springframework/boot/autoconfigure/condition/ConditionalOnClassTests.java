/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnClass}.
 *
 * @author Dave Syer
 */
public class ConditionalOnClassTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testVanillaOnClassCondition() {
		this.context.register(BasicConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	public void testMissingOnClassCondition() {
		this.context.register(MissingConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isFalse();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	public void testOnClassConditionWithXml() {
		this.context.register(BasicConfiguration.class, XmlConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	public void testOnClassConditionWithCombinedXml() {
		this.context.register(CombinedXmlConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBean("bar")).isEqualTo("bar");
	}

	@Configuration
	@ConditionalOnClass(ConditionalOnClassTests.class)
	protected static class BasicConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnClass(name = "FOO")
	protected static class MissingConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	protected static class FooConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	protected static class XmlConfiguration {

	}

	@Configuration
	@Import(BasicConfiguration.class)
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	protected static class CombinedXmlConfiguration {

	}

}
