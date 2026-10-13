/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextIdApplicationContextInitializer}.
 *
 * @author Dave Syer
 */
class ContextIdApplicationContextInitializerTests {

	private final ContextIdApplicationContextInitializer initializer = new ContextIdApplicationContextInitializer();

	private final List<ConfigurableApplicationContext> contexts = new ArrayList<>();

	@AfterEach
	void closeContexts() {
		Collections.reverse(this.contexts);
		this.contexts.forEach(ConfigurableApplicationContext::close);
	}

	@Test
	void singleContextWithDefaultName() {
		ConfigurableApplicationContext context = createContext(null);
		assertThat(context.getId()).isEqualTo("application");
	}

	@Test
	void singleContextWithCustomName() {
		ConfigurableApplicationContext context = createContext(null, "spring.application.name=test");
		assertThat(context.getId()).isEqualTo("test");
	}

	@Test
	void linearHierarchy() {
		ConfigurableApplicationContext grandparent = createContext(null);
		ConfigurableApplicationContext parent = createContext(grandparent);
		ConfigurableApplicationContext child = createContext(parent);
		assertThat(child.getId()).isEqualTo("application-1-1");
	}

	@Test
	void complexHierarchy() {
		ConfigurableApplicationContext grandparent = createContext(null);
		ConfigurableApplicationContext parent1 = createContext(grandparent);
		ConfigurableApplicationContext parent2 = createContext(grandparent);
		ConfigurableApplicationContext child1_1 = createContext(parent1);
		assertThat(child1_1.getId()).isEqualTo("application-1-1");
		ConfigurableApplicationContext child1_2 = createContext(parent1);
		assertThat(child1_2.getId()).isEqualTo("application-1-2");
		ConfigurableApplicationContext child2_1 = createContext(parent2);
		assertThat(child2_1.getId()).isEqualTo("application-2-1");
	}

	@Test
	void contextWithParentWithNoContextIdFallsBackToDefaultId() {
		ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext();
		this.contexts.add(parent);
		parent.refresh();
		assertThat(createContext(parent).getId()).isEqualTo("application");
	}

	private ConfigurableApplicationContext createContext(@Nullable ConfigurableApplicationContext parent,
			String... properties) {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, properties);
		if (parent != null) {
			context.setParent(parent);
		}
		this.initializer.initialize(context);
		context.refresh();
		this.contexts.add(context);
		return context;
	}

}
