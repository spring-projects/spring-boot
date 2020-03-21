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

package org.springframework.boot.autoconfigure.web.embedded;

import java.time.Duration;
import java.util.Map;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link NettyWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Artsiom Yudovin
 */
class NettyWebServerFactoryCustomizerTests {

	private MockEnvironment environment;

	private ServerProperties serverProperties;

	private NettyWebServerFactoryCustomizer customizer;

	@Captor
	private ArgumentCaptor<NettyServerCustomizer> customizerCaptor;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		this.environment = new MockEnvironment();
		this.serverProperties = new ServerProperties();
		ConfigurationPropertySources.attach(this.environment);
		this.customizer = new NettyWebServerFactoryCustomizer(this.environment, this.serverProperties);
	}

	@Test
	void deduceUseForwardHeaders() {
		this.environment.setProperty("DYNO", "-");
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	@Test
	void defaultUseForwardHeaders() {
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(false);
	}

	@Test
	void setUseForwardHeaders() {
		this.serverProperties.setUseForwardHeaders(true);
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	@Test
	void forwardHeadersWhenStrategyIsNativeShouldConfigureValve() {
		this.serverProperties.setForwardHeadersStrategy(ServerProperties.ForwardHeadersStrategy.NATIVE);
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	@Test
	void forwardHeadersWhenStrategyIsNoneShouldNotConfigureValve() {
		this.environment.setProperty("DYNO", "-");
		this.serverProperties.setForwardHeadersStrategy(ServerProperties.ForwardHeadersStrategy.NONE);
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(false);
	}

	@Test
	void setServerConnectionTimeoutAsZero() {
		setupServerConnectionTimeout(Duration.ZERO);
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verifyConnectionTimeout(factory, null);
	}

	@Test
	void setServerConnectionTimeoutAsMinusOne() {
		setupServerConnectionTimeout(Duration.ofNanos(-1));
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verifyConnectionTimeout(factory, 0);
	}

	@Test
	void setServerConnectionTimeout() {
		setupServerConnectionTimeout(Duration.ofSeconds(1));
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verifyConnectionTimeout(factory, 1000);
	}

	@Test
	void setConnectionTimeout() {
		setupConnectionTimeout(Duration.ofSeconds(1));
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verifyConnectionTimeout(factory, 1000);
	}

	@SuppressWarnings("unchecked")
	private void verifyConnectionTimeout(NettyReactiveWebServerFactory factory, Integer expected) {
		if (expected == null) {
			verify(factory, never()).addServerCustomizers(any(NettyServerCustomizer.class));
			return;
		}
		verify(factory, times(1)).addServerCustomizers(this.customizerCaptor.capture());
		NettyServerCustomizer serverCustomizer = this.customizerCaptor.getValue();
		HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
		TcpServer tcpConfiguration = ReflectionTestUtils.invokeMethod(httpServer, "tcpConfiguration");
		ServerBootstrap bootstrap = tcpConfiguration.configure();
		Map<Object, Object> options = (Map<Object, Object>) ReflectionTestUtils.getField(bootstrap, "options");
		assertThat(options).containsEntry(ChannelOption.CONNECT_TIMEOUT_MILLIS, expected);
	}

	private void setupServerConnectionTimeout(Duration connectionTimeout) {
		this.serverProperties.setUseForwardHeaders(null);
		this.serverProperties.setMaxHttpHeaderSize(null);
		this.serverProperties.setConnectionTimeout(connectionTimeout);
	}

	private void setupConnectionTimeout(Duration connectionTimeout) {
		this.serverProperties.setUseForwardHeaders(null);
		this.serverProperties.setMaxHttpHeaderSize(null);
		this.serverProperties.getNetty().setConnectionTimeout(connectionTimeout);
	}

}
