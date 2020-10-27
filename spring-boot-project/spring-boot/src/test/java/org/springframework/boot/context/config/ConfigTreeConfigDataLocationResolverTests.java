/*
 * Copyright 2012-2020 the original author or authors.
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

	private ConfigTreeConfigDataLocationResolver resolver = new ConfigTreeConfigDataLocationResolver(
			new DefaultResourceLoader());

	private ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

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
		assertThat(locations.size()).isEqualTo(1);
		assertThat(locations).extracting(Object::toString)
				.containsExactly("config tree [" + new File("/etc/config").getAbsolutePath() + "]");
	}

	@Test
	void resolveWilcardPattern() throws Exception {
		File directoryA = new File(this.temp, "a");
		File directoryB = new File(this.temp, "b");
		directoryA.mkdirs();
		directoryB.mkdirs();
		FileCopyUtils.copy("test".getBytes(), new File(directoryA, "spring"));
		FileCopyUtils.copy("test".getBytes(), new File(directoryB, "boot"));
		List<ConfigTreeConfigDataResource> locations = this.resolver.resolve(this.context,
				ConfigDataLocation.of("configtree:" + this.temp.getAbsolutePath() + "/*/"));
		assertThat(locations.size()).isEqualTo(2);
		assertThat(locations).extracting(Object::toString).containsExactly(
				"config tree [" + directoryA.getAbsolutePath() + "]",
				"config tree [" + directoryB.getAbsolutePath() + "]");
	}

}
