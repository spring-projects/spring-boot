/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UnboundElementsSourceFilter}.
 *
 * @author Madhura Bhave
 */
class UnboundElementsSourceFilterTests {

	private UnboundElementsSourceFilter filter;

	private ConfigurationPropertySource source;

	@BeforeEach
	void setUp() {
		this.filter = new UnboundElementsSourceFilter();
		this.source = mock(ConfigurationPropertySource.class);
	}

	@Test
	void filterWhenSourceIsSystemPropertiesPropertySourceShouldReturnFalse() {
		MockPropertySource propertySource = new MockPropertySource(
				StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		given(this.source.getUnderlyingSource()).willReturn(propertySource);
		assertThat(this.filter.apply(this.source)).isFalse();
	}

	@Test
	void filterWhenSourceIsSystemEnvironmentPropertySourceShouldReturnFalse() {
		MockPropertySource propertySource = new MockPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		given(this.source.getUnderlyingSource()).willReturn(propertySource);
		assertThat(this.filter.apply(this.source)).isFalse();
	}

	@Test
	void filterWhenSourceIsNotSystemShouldReturnTrue() {
		MockPropertySource propertySource = new MockPropertySource("test");
		given(this.source.getUnderlyingSource()).willReturn(propertySource);
		assertThat(this.filter.apply(this.source)).isTrue();
	}

}
