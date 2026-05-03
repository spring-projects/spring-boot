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

package org.springframework.boot.reactor.netty.autoconfigure;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.netty.http.HttpDecoderSpec;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyServerProperties}.
 *
 * @author Andy Wilkinson
 */
class NettyServerPropertiesTests {

	private final NettyServerProperties properties = new NettyServerProperties();

	@Test
	void testCustomizeNettyIdleTimeout() {
		bind("server.netty.idle-timeout", "10s");
		assertThat(this.properties.getIdleTimeout()).isEqualTo(Duration.ofSeconds(10));
	}

	@Test
	void testCustomizeNettyMaxKeepAliveRequests() {
		bind("server.netty.max-keep-alive-requests", "100");
		assertThat(this.properties.getMaxKeepAliveRequests()).isEqualTo(100);
	}

	@Test
	void nettyMaxInitialLineLengthMatchesHttpDecoderSpecDefault() {
		assertThat(this.properties.getMaxInitialLineLength().toBytes())
			.isEqualTo(HttpDecoderSpec.DEFAULT_MAX_INITIAL_LINE_LENGTH);
	}

	@Test
	void nettyValidateHeadersMatchesHttpDecoderSpecDefault() {
		assertThat(this.properties.isValidateHeaders()).isTrue();
	}

	@Test
	void nettyH2cMaxContentLengthMatchesHttpDecoderSpecDefault() {
		assertThat(this.properties.getH2cMaxContentLength().toBytes()).isZero();
	}

	@Test
	void nettyInitialBufferSizeMatchesHttpDecoderSpecDefault() {
		assertThat(this.properties.getInitialBufferSize().toBytes())
			.isEqualTo(HttpDecoderSpec.DEFAULT_INITIAL_BUFFER_SIZE);
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("server.netty", Bindable.ofInstance(this.properties));
	}

}
