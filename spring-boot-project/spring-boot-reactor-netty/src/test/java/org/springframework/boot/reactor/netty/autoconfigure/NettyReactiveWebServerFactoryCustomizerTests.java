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
import java.util.Map;

import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.netty.http.Http2SettingsSpec;
import reactor.netty.http.server.HttpRequestDecoderSpec;
import reactor.netty.http.server.HttpServer;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.reactor.netty.NettyServerCustomizer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link NettyReactiveWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Artsiom Yudovin
 * @author Leo Li
 */
@ExtendWith(MockitoExtension.class)
class NettyReactiveWebServerFactoryCustomizerTests {

	private final MockEnvironment environment = new MockEnvironment();

	private final ServerProperties serverProperties = new ServerProperties();

	private final NettyServerProperties nettyProperties = new NettyServerProperties();

	private NettyReactiveWebServerFactoryCustomizer customizer;

	@Captor
	private ArgumentCaptor<NettyServerCustomizer> customizerCaptor;

	@BeforeEach
	void setup() {
		ConfigurationPropertySources.attach(this.environment);
		this.customizer = new NettyReactiveWebServerFactoryCustomizer(this.environment, this.serverProperties,
				this.nettyProperties);
	}

	@Test
	void deduceUseForwardHeaders() {
		this.environment.setProperty("DYNO", "-");
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().setUseForwardHeaders(true);
	}

	@Test
	void defaultUseForwardHeaders() {
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().setUseForwardHeaders(false);
	}

	@Test
	void forwardHeadersWhenStrategyIsNativeShouldConfigureValve() {
		this.serverProperties.setForwardHeadersStrategy(ServerProperties.ForwardHeadersStrategy.NATIVE);
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().setUseForwardHeaders(true);
	}

	@Test
	void forwardHeadersWhenStrategyIsNoneShouldNotConfigureValve() {
		this.environment.setProperty("DYNO", "-");
		this.serverProperties.setForwardHeadersStrategy(ServerProperties.ForwardHeadersStrategy.NONE);
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().setUseForwardHeaders(false);
	}

	@Test
	void setConnectionTimeout() {
		this.nettyProperties.setConnectionTimeout(Duration.ofSeconds(1));
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verifyConnectionTimeout(factory, 1000);
	}

	@Test
	void setIdleTimeout() {
		this.nettyProperties.setIdleTimeout(Duration.ofSeconds(1));
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verifyIdleTimeout(factory, Duration.ofSeconds(1));
	}

	@Test
	void setMaxKeepAliveRequests() {
		this.nettyProperties.setMaxKeepAliveRequests(100);
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verifyMaxKeepAliveRequests(factory, 100);
	}

	@Test
	void setHttp2MaxRequestHeaderSize() {
		DataSize headerSize = DataSize.ofKilobytes(24);
		this.serverProperties.getHttp2().setEnabled(true);
		this.serverProperties.setMaxHttpRequestHeaderSize(headerSize);
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		verifyHttp2MaxHeaderSize(factory, headerSize.toBytes());
	}

	@Test
	void configureHttpRequestDecoder() {
		NettyServerProperties nettyProperties = this.nettyProperties;
		this.serverProperties.setMaxHttpRequestHeaderSize(DataSize.ofKilobytes(24));
		nettyProperties.setValidateHeaders(false);
		nettyProperties.setInitialBufferSize(DataSize.ofBytes(512));
		nettyProperties.setH2cMaxContentLength(DataSize.ofKilobytes(1));
		nettyProperties.setMaxInitialLineLength(DataSize.ofKilobytes(32));
		NettyReactiveWebServerFactory factory = mock(NettyReactiveWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().addServerCustomizers(this.customizerCaptor.capture());
		NettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
		HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
		HttpRequestDecoderSpec decoder = httpServer.configuration().decoder();
		assertThat(decoder.validateHeaders()).isFalse();
		assertThat(decoder.maxHeaderSize()).isEqualTo(this.serverProperties.getMaxHttpRequestHeaderSize().toBytes());
		assertThat(decoder.initialBufferSize()).isEqualTo(nettyProperties.getInitialBufferSize().toBytes());
		assertThat(decoder.h2cMaxContentLength()).isEqualTo(nettyProperties.getH2cMaxContentLength().toBytes());
		assertThat(decoder.maxInitialLineLength()).isEqualTo(nettyProperties.getMaxInitialLineLength().toBytes());
	}

	private void verifyConnectionTimeout(NettyReactiveWebServerFactory factory, Integer expected) {
		if (expected == null) {
			then(factory).should(never()).addServerCustomizers(any(NettyServerCustomizer.class));
			return;
		}
		then(factory).should(times(2)).addServerCustomizers(this.customizerCaptor.capture());
		NettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
		HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
		Map<ChannelOption<?>, ?> options = httpServer.configuration().options();
		assertThat(options.get(ChannelOption.CONNECT_TIMEOUT_MILLIS)).isEqualTo(expected);
	}

	private void verifyIdleTimeout(NettyReactiveWebServerFactory factory, Duration expected) {
		if (expected == null) {
			then(factory).should(never()).addServerCustomizers(any(NettyServerCustomizer.class));
			return;
		}
		then(factory).should(times(2)).addServerCustomizers(this.customizerCaptor.capture());
		NettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
		HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
		Duration idleTimeout = httpServer.configuration().idleTimeout();
		assertThat(idleTimeout).isEqualTo(expected);
	}

	private void verifyMaxKeepAliveRequests(NettyReactiveWebServerFactory factory, int expected) {
		then(factory).should(times(2)).addServerCustomizers(this.customizerCaptor.capture());
		NettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
		HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
		int maxKeepAliveRequests = httpServer.configuration().maxKeepAliveRequests();
		assertThat(maxKeepAliveRequests).isEqualTo(expected);
	}

	private void verifyHttp2MaxHeaderSize(NettyReactiveWebServerFactory factory, long expected) {
		then(factory).should(times(2)).addServerCustomizers(this.customizerCaptor.capture());
		NettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
		HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
		Http2SettingsSpec decoder = httpServer.configuration().http2SettingsSpec();
		assertThat(decoder.maxHeaderListSize()).isEqualTo(expected);
	}

}
