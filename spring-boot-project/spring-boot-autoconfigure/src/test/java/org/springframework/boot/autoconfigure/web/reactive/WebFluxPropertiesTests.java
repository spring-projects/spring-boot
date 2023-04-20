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

package org.springframework.boot.autoconfigure.web.reactive;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxProperties}
 *
 * @author Brian Clozel
 */
class WebFluxPropertiesTests {

	private final WebFluxProperties properties = new WebFluxProperties();

	@Test
	void shouldPrefixBasePathWithMissingSlash() {
		bind("spring.webflux.base-path", "something");
		assertThat(this.properties.getBasePath()).isEqualTo("/something");
	}

	@Test
	void shouldRemoveTrailingSlashFromBasePath() {
		bind("spring.webflux.base-path", "/something/");
		assertThat(this.properties.getBasePath()).isEqualTo("/something");
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("spring.webflux", Bindable.ofInstance(this.properties));
	}

}
