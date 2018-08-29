/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.embedded;

import org.junit.Before;
import org.junit.Test;
import reactor.netty.http.server.HttpRequestDecoderSpec;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link NettyWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Samuel Ko
 */
public class NettyWebServerFactoryCustomizerTests {

	private MockEnvironment environment;

	private ServerProperties serverProperties;

	private NettyWebServerFactoryCustomizer customizer;

	@Before
	public void setup() {
		this.environment = new MockEnvironment();
		this.serverProperties = new ServerProperties();
		ConfigurationPropertySources.attach(this.environment);
		this.customizer = new NettyWebServerFactoryCustomizer(this.environment,
				this.serverProperties);
	}

	@Test
	public void deduceUseForwardHeaders() {
		this.environment.setProperty("DYNO", "-");
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	@Test
	public void defaultUseForwardHeaders() {
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(false);
	}

	@Test
	public void setUseForwardHeaders() {
		this.serverProperties.setUseForwardHeaders(true);
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	@Test
	public void customizeHttpRequestDecoderSpec() {
		this.serverProperties.getNetty().setMaxChunkSize(1);
		this.serverProperties.getNetty().setMaxHeaderSize(1);
		this.serverProperties.getNetty().setInitialBufferSize(1);
		this.serverProperties.getNetty().setMaxInitialLineLength(1);
		this.serverProperties.getNetty().setValidateHeaders(false);

		NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
		HttpServer httpServer = HttpServer.from(mock(TcpServer.class));

		this.customizer.customize(factory);
		this.customizer.httpRequestDecoderSpecMapper = this.customizer.httpRequestDecoderSpecMapper
				.andThen((httpRequestDecoderSpec) -> {
					assertThat(httpRequestDecoderSpec)
							.extracting("maxInitialLineLength", "maxHeaderSize",
									"maxChunkSize", "validateHeaders",
									"initialBufferSize")
							.describedAs("custom HttpRequestDecoderSpec configuration")
							.containsExactly(1, 1, 1, false, 1);
					return httpRequestDecoderSpec;
				});

		for (NettyServerCustomizer customizer : factory.getServerCustomizers()) {
			httpServer = customizer.apply(httpServer);
		}
	}

	@Test
	public void defaultHttpRequestDecoderSpec() {
		NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
		HttpServer httpServer = HttpServer.from(mock(TcpServer.class));

		this.customizer.customize(factory);

		this.customizer.httpRequestDecoderSpecMapper = this.customizer.httpRequestDecoderSpecMapper
				.andThen((httpRequestDecoderSpec) -> {
					assertThat(httpRequestDecoderSpec)
							.extracting("maxInitialLineLength", "maxHeaderSize",
									"maxChunkSize", "validateHeaders",
									"initialBufferSize")
							.describedAs("default HttpRequestDecoderSpec configuration")
							.containsExactly(
									HttpRequestDecoderSpec.DEFAULT_MAX_INITIAL_LINE_LENGTH,
									HttpRequestDecoderSpec.DEFAULT_MAX_HEADER_SIZE,
									HttpRequestDecoderSpec.DEFAULT_MAX_CHUNK_SIZE,
									HttpRequestDecoderSpec.DEFAULT_VALIDATE_HEADERS,
									HttpRequestDecoderSpec.DEFAULT_INITIAL_BUFFER_SIZE);
					return httpRequestDecoderSpec;
				});

		for (NettyServerCustomizer customizer : factory.getServerCustomizers()) {
			httpServer = customizer.apply(httpServer);
		}
	}

}
