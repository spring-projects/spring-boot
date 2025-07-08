/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.undertow.autoconfigure;

import java.util.Collections;
import java.util.Map;

import io.undertow.UndertowOptions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UndertowServerProperties}.
 *
 * @author Andy Wilkinson
 */
class UndertowServerPropertiesTests {

	private final UndertowServerProperties properties = new UndertowServerProperties();

	@Test
	void testCustomizeUndertowServerOption() {
		bind("server.undertow.options.server.ALWAYS_SET_KEEP_ALIVE", "true");
		assertThat(this.properties.getOptions().getServer()).containsEntry("ALWAYS_SET_KEEP_ALIVE", "true");
	}

	@Test
	void testCustomizeUndertowSocketOption() {
		bind("server.undertow.options.socket.ALWAYS_SET_KEEP_ALIVE", "true");
		assertThat(this.properties.getOptions().getSocket()).containsEntry("ALWAYS_SET_KEEP_ALIVE", "true");
	}

	@Test
	void testCustomizeUndertowIoThreads() {
		bind("server.undertow.threads.io", "4");
		assertThat(this.properties.getThreads().getIo()).isEqualTo(4);
	}

	@Test
	void testCustomizeUndertowWorkerThreads() {
		bind("server.undertow.threads.worker", "10");
		assertThat(this.properties.getThreads().getWorker()).isEqualTo(10);
	}

	@Test
	void undertowMaxHttpPostSizeMatchesDefault() {
		assertThat(this.properties.getMaxHttpPostSize().toBytes()).isEqualTo(UndertowOptions.DEFAULT_MAX_ENTITY_SIZE);
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("server.undertow", Bindable.ofInstance(this.properties));
	}

}
