/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLoader}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLoaderTests {

	private final TestConfigDataLoader loader = new TestConfigDataLoader();

	private final ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);

	@Test
	void isLoadableAlwaysReturnsTrue() {
		assertThat(this.loader.isLoadable(this.context, new TestConfigDataResource())).isTrue();
	}

	static class TestConfigDataLoader implements ConfigDataLoader<TestConfigDataResource> {

		@Override
		public @Nullable ConfigData load(ConfigDataLoaderContext context, TestConfigDataResource resource)
				throws IOException {
			return null;
		}

	}

	static class TestConfigDataResource extends ConfigDataResource {

	}

}
