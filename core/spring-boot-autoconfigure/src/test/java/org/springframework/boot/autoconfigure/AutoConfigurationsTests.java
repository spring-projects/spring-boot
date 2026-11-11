/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.annotation.Configurations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigurations}.
 *
 * @author Phillip Webb
 */
class AutoConfigurationsTests {

	@Test
	void ofShouldCreateOrderedConfigurations() {
		Configurations configurations = AutoConfigurations.of(AutoConfigureA.class, AutoConfigureB.class);
		assertThat(Configurations.getClasses(configurations)).containsExactly(AutoConfigureB.class,
				AutoConfigureA.class);
	}

	@Test
	void whenHasReplacementForAutoConfigureAfterShouldCreateOrderedConfigurations() {
		Configurations configurations = new AutoConfigurations(this::replaceB,
				Arrays.asList(AutoConfigureA.class, AutoConfigureB2.class));
		assertThat(Configurations.getClasses(configurations)).containsExactly(AutoConfigureB2.class,
				AutoConfigureA.class);
	}

	@Test
	void whenHasReplacementForClassShouldReplaceClass() {
		Configurations configurations = new AutoConfigurations(this::replaceB,
				Arrays.asList(AutoConfigureA.class, AutoConfigureB.class));
		assertThat(Configurations.getClasses(configurations)).containsExactly(AutoConfigureB2.class,
				AutoConfigureA.class);
	}

	@Test
	void getBeanNameShouldUseClassName() {
		Configurations configurations = AutoConfigurations.of(AutoConfigureA.class, AutoConfigureB.class);
		assertThat(configurations.getBeanName(AutoConfigureA.class)).isEqualTo(AutoConfigureA.class.getName());
	}

	private String replaceB(String className) {
		return (!AutoConfigureB.class.getName().equals(className)) ? className : AutoConfigureB2.class.getName();
	}

	@AutoConfigureAfter(AutoConfigureB.class)
	static class AutoConfigureA {

	}

	static class AutoConfigureB {

	}

	static class AutoConfigureB2 {

	}

}
