/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.net.protocol.jar;

import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.net.protocol.Handlers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarFileUrlKey}.
 *
 * @author Phillip Webb
 */
class JarFileUrlKeyTests {

	@BeforeAll
	static void setup() {
		Handlers.register();
	}

	@Test
	void getCreatesKey() throws Exception {
		URL url = new URL("jar:nested:/my.jar/!mynested.jar!/my/path");
		assertThat(JarFileUrlKey.get(url)).isEqualTo("jar:nested:/my.jar/!mynested.jar!/my/path");
	}

	@Test
	void getWhenUppercaseProtocolCreatesKey() throws Exception {
		URL url = new URL("JAR:nested:/my.jar/!mynested.jar!/my/path");
		assertThat(JarFileUrlKey.get(url)).isEqualTo("jar:nested:/my.jar/!mynested.jar!/my/path");
	}

	@Test
	void getWhenHasHostAndPortCreatesKey() throws Exception {
		URL url = new URL("https://example.com:1234/test");
		assertThat(JarFileUrlKey.get(url)).isEqualTo("https:example.com:1234/test");
	}

	@Test
	void getWhenHasUppercaseHostCreatesKey() throws Exception {
		URL url = new URL("https://EXAMPLE.com:1234/test");
		assertThat(JarFileUrlKey.get(url)).isEqualTo("https:example.com:1234/test");
	}

	@Test
	void getWhenHasNoPortCreatesKeyWithDefaultPort() throws Exception {
		URL url = new URL("https://EXAMPLE.com/test");
		assertThat(JarFileUrlKey.get(url)).isEqualTo("https:example.com:443/test");
	}

	@Test
	void getWhenHasNoFileCreatesKey() throws Exception {
		URL url = new URL("https://EXAMPLE.com");
		assertThat(JarFileUrlKey.get(url)).isEqualTo("https:example.com:443");
	}

	@Test
	void getWhenHasRuntimeRefCreatesKey() throws Exception {
		URL url = new URL("jar:nested:/my.jar/!mynested.jar!/my/path#runtime");
		assertThat(JarFileUrlKey.get(url)).isEqualTo("jar:nested:/my.jar/!mynested.jar!/my/path#runtime");
	}

	@Test
	void getWhenHasOtherRefCreatesKeyWithoutRef() throws Exception {
		URL url = new URL("jar:nested:/my.jar/!mynested.jar!/my/path#example");
		assertThat(JarFileUrlKey.get(url)).isEqualTo("jar:nested:/my.jar/!mynested.jar!/my/path");
	}

}
