/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationPropertySource}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourceTests {

	@Test
	void fromCreatesConfigurationPropertySourcesPropertySource() {
		MockPropertySource source = new MockPropertySource();
		source.setProperty("spring", "boot");
		ConfigurationPropertySource adapted = ConfigurationPropertySource.from(source);
		assertThat(adapted).isNotNull();
		ConfigurationProperty spring = adapted.getConfigurationProperty(ConfigurationPropertyName.of("spring"));
		assertThat(spring).isNotNull();
		assertThat(spring.getValue()).isEqualTo("boot");
	}

	@Test
	void fromWhenSourceIsAlreadyConfigurationPropertySourcesPropertySourceReturnsNull() {
		ConfigurationPropertySourcesPropertySource source = mock(ConfigurationPropertySourcesPropertySource.class);
		assertThat(ConfigurationPropertySource.from(source)).isNull();
	}

}
