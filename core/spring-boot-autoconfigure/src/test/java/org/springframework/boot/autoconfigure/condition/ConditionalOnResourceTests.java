/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnResource @ConditionalOnResource}.
 *
 * @author Dave Syer
 */
class ConditionalOnResourceTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	@WithResource(name = "schema.sql")
	void testResourceExists() {
		this.context.register(BasicConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isTrue();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	@WithResource(name = "schema.sql")
	void testResourceExistsWithPlaceholder() {
		TestPropertyValues.of("schema=schema.sql").applyTo(this.context);
		this.context.register(PlaceholderConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isTrue();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	void testResourceNotExists() {
		this.context.register(MissingConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnResource(resources = "foo")
	static class MissingConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnResource(resources = "schema.sql")
	static class BasicConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnResource(resources = "${schema}")
	static class PlaceholderConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

}
