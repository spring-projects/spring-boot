/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link CachingConfigurationPropertySource}.
 *
 * @author Phillip Webb
 */
class CachingConfigurationPropertySourceTests {

	@Test
	void findWhenNullSourceReturnsNull() {
		ConfigurationPropertySource source = null;
		assertThat(CachingConfigurationPropertySource.find(source)).isNull();
	}

	@Test
	void findWhenNotCachingConfigurationPropertySourceReturnsNull() {
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class);
		assertThat(CachingConfigurationPropertySource.find(source)).isNull();
	}

	@Test
	void findWhenCachingConfigurationPropertySourceReturnsCaching() {
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class,
				withSettings().extraInterfaces(CachingConfigurationPropertySource.class));
		ConfigurationPropertyCaching caching = mock(ConfigurationPropertyCaching.class);
		given(((CachingConfigurationPropertySource) source).getCaching()).willReturn(caching);
		assertThat(CachingConfigurationPropertySource.find(source)).isEqualTo(caching);
	}

}
