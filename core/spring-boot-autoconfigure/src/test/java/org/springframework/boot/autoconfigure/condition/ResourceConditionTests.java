/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ResourceCondition}.
 *
 * @author Stephane Nicoll
 */
class ResourceConditionTests {

	private @Nullable ConfigurableApplicationContext context;

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	@WithResource(name = "logging.properties")
	void defaultResourceAndNoExplicitKey() {
		load(DefaultLocationConfiguration.class);
		assertThat(isContainsBean()).isTrue();
	}

	@Test
	void unknownDefaultLocationAndNoExplicitKey() {
		load(UnknownDefaultLocationConfiguration.class);
		assertThat(isContainsBean()).isFalse();
	}

	@Test
	void unknownDefaultLocationAndExplicitKeyToResource() {
		load(UnknownDefaultLocationConfiguration.class, "spring.foo.test.config=logging.properties");
		assertThat(isContainsBean()).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(applicationContext);
		applicationContext.register(config);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	private boolean isContainsBean() {
		assertThat(this.context).isNotNull();
		return this.context.containsBean("foo");
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(DefaultLocationResourceCondition.class)
	static class DefaultLocationConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(UnknownDefaultLocationResourceCondition.class)
	static class UnknownDefaultLocationConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	static class DefaultLocationResourceCondition extends ResourceCondition {

		DefaultLocationResourceCondition() {
			super("test", "spring.foo.test.config", "classpath:/logging.properties");
		}

	}

	static class UnknownDefaultLocationResourceCondition extends ResourceCondition {

		UnknownDefaultLocationResourceCondition() {
			super("test", "spring.foo.test.config", "classpath:/this-file-does-not-exist.xml");
		}

	}

}
