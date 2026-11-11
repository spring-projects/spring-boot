/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.example.ExampleConfig;
import org.springframework.boot.test.context.example.scan.Example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AnnotatedClassFinder}.
 *
 * @author Phillip Webb
 */
class AnnotatedClassFinderTests {

	private final AnnotatedClassFinder finder = new AnnotatedClassFinder(SpringBootConfiguration.class);

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void findFromClassWhenSourceIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.finder.findFromClass((Class<?>) null))
			.withMessageContaining("'source' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void findFromPackageWhenSourceIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.finder.findFromPackage((String) null))
			.withMessageContaining("'source' must not be null");
	}

	@Test
	void findFromPackageWhenNoConfigurationFoundShouldReturnNull() {
		Class<?> config = this.finder.findFromPackage("org.springframework.boot");
		assertThat(config).isNull();
	}

	@Test
	void findFromClassWhenConfigurationIsFoundShouldReturnConfiguration() {
		Class<?> config = this.finder.findFromClass(Example.class);
		assertThat(config).isEqualTo(ExampleConfig.class);
	}

	@Test
	void findFromPackageWhenConfigurationIsFoundShouldReturnConfiguration() {
		Class<?> config = this.finder.findFromPackage("org.springframework.boot.test.context.example.scan");
		assertThat(config).isEqualTo(ExampleConfig.class);
	}

}
