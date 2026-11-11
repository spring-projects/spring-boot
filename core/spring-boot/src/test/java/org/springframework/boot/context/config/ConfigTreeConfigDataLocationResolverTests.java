/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigTreeConfigDataLocationResolver}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigTreeConfigDataLocationResolverTests {

	private final ConfigTreeConfigDataLocationResolver resolver = new ConfigTreeConfigDataLocationResolver(
			new DefaultResourceLoader());

	private final ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

	@TempDir
	File temp;

	@Test
	void isResolvableWhenPrefixMatchesReturnsTrue() {
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("configtree:/etc/config"))).isTrue();
	}

	@Test
	void isResolvableWhenPrefixDoesNotMatchReturnsFalse() {
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("http://etc/config"))).isFalse();
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("/etc/config"))).isFalse();
	}

	@Test
	void resolveReturnsConfigVolumeMountLocation() {
		List<ConfigTreeConfigDataResource> locations = this.resolver.resolve(this.context,
				ConfigDataLocation.of("configtree:/etc/config/"));
		assertThat(locations).hasSize(1);
		assertThat(locations).extracting(Object::toString)
			.containsExactly("config tree [" + new File("/etc/config").getAbsolutePath() + "]");
	}

	@Test
	void resolveWildcardPattern() throws Exception {
		File directoryA = new File(this.temp, "a");
		File directoryB = new File(this.temp, "b");
		directoryA.mkdirs();
		directoryB.mkdirs();
		FileCopyUtils.copy("test".getBytes(), new File(directoryA, "spring"));
		FileCopyUtils.copy("test".getBytes(), new File(directoryB, "boot"));
		List<ConfigTreeConfigDataResource> locations = this.resolver.resolve(this.context,
				ConfigDataLocation.of("configtree:" + this.temp.getAbsolutePath() + "/*/"));
		assertThat(locations).hasSize(2);
		assertThat(locations).extracting(Object::toString)
			.containsExactly("config tree [" + directoryA.getAbsolutePath() + "]",
					"config tree [" + directoryB.getAbsolutePath() + "]");
	}

}
