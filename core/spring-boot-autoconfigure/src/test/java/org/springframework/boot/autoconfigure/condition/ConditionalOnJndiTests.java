/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.jndi.JndiPropertiesHidingClassLoader;
import org.springframework.boot.autoconfigure.jndi.TestableInitialContextFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionalOnJndi @ConditionalOnJndi}
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ConditionalOnJndiTests {

	private ClassLoader threadContextClassLoader;

	private @Nullable String initialContextFactory;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	private final MockableOnJndi condition = new MockableOnJndi();

	@BeforeEach
	void setupThreadContextClassLoader() {
		this.threadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new JndiPropertiesHidingClassLoader(getClass().getClassLoader()));
	}

	@AfterEach
	void close() {
		TestableInitialContextFactory.clearAll();
		if (this.initialContextFactory != null) {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY, this.initialContextFactory);
		}
		else {
			System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
		}
		Thread.currentThread().setContextClassLoader(this.threadContextClassLoader);
	}

	@Test
	void jndiNotAvailable() {
		this.contextRunner.withUserConfiguration(JndiAvailableConfiguration.class, JndiConditionConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	void jndiAvailable() {
		setupJndi();
		this.contextRunner.withUserConfiguration(JndiAvailableConfiguration.class, JndiConditionConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(String.class));
	}

	@Test
	void jndiLocationNotBound() {
		setupJndi();
		this.contextRunner.withUserConfiguration(JndiConditionConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	void jndiLocationBound() {
		setupJndi();
		TestableInitialContextFactory.bind("java:/FooManager", new Object());
		this.contextRunner.withUserConfiguration(JndiConditionConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(String.class));
	}

	@Test
	void jndiLocationNotFound() {
		ConditionOutcome outcome = this.condition.getMatchOutcome(mock(ConditionContext.class),
				mockMetadata("java:/a"));
		assertThat(outcome.isMatch()).isFalse();
	}

	@Test
	void jndiLocationFound() {
		this.condition.setFoundLocation("java:/b");
		ConditionOutcome outcome = this.condition.getMatchOutcome(mock(ConditionContext.class),
				mockMetadata("java:/a", "java:/b"));
		assertThat(outcome.isMatch()).isTrue();
	}

	private void setupJndi() {
		this.initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestableInitialContextFactory.class.getName());
	}

	private AnnotatedTypeMetadata mockMetadata(String... value) {
		AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("value", value);
		given(metadata.getAnnotationAttributes(ConditionalOnJndi.class.getName())).willReturn(attributes);
		return metadata;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJndi
	static class JndiAvailableConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJndi("java:/FooManager")
	static class JndiConditionConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	static class MockableOnJndi extends OnJndiCondition {

		private final boolean jndiAvailable = true;

		private @Nullable String foundLocation;

		@Override
		protected boolean isJndiAvailable() {
			return this.jndiAvailable;
		}

		@Override
		protected JndiLocator getJndiLocator(String[] locations) {
			return new JndiLocator(locations) {
				@Override
				public @Nullable String lookupFirstLocation() {
					return MockableOnJndi.this.foundLocation;
				}
			};
		}

		void setFoundLocation(String foundLocation) {
			this.foundLocation = foundLocation;
		}

	}

}
