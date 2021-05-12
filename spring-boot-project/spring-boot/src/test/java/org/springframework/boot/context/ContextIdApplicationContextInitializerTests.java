/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	private List<ConfigurableApplicationContext> contexts = new ArrayList<>();

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

	private ConfigurableApplicationContext createContext(ConfigurableApplicationContext parent, String... properties) {
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
