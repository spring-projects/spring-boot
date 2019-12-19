/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.rsocket.netty;

import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Arrays;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;
import reactor.test.StepVerifier;

import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServer.Transport;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryProcessor;
import org.springframework.boot.web.server.Ssl;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NettyRSocketServerFactory}
 *
 * @author Brian Clozel
 * @author Leo Li
 * @author Chris Bono
 */
class NettyRSocketServerFactoryTests {

	private NettyRSocketServer server;

	private RSocketRequester requester;

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	@AfterEach
	void tearDown() {
		if (this.server != null) {
			try {
				this.server.stop();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		if (this.requester != null) {
			this.requester.rsocket().dispose();
		}
	}

	private NettyRSocketServerFactory getFactory() {
		return new NettyRSocketServerFactory();
	}

	@Test
	void specificPort() {
		NettyRSocketServerFactory factory = getFactory();
		int specificPort = SocketUtils.findAvailableTcpPort(41000);
		factory.setPort(specificPort);
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = createRSocketTcpClient(false);
		String payload = "test payload";
		String response = this.requester.route("test").data(payload).retrieveMono(String.class).block(TIMEOUT);
		assertThat(this.server.address().getPort()).isEqualTo(specificPort);
		assertThat(response).isEqualTo(payload);
	}

	@Test
	void websocketTransport() {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(RSocketServer.Transport.WEBSOCKET);
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = createRSocketWebSocketClient(false);
		String payload = "test payload";
		String response = this.requester.route("test").data(payload).retrieveMono(String.class).block(TIMEOUT);
		assertThat(response).isEqualTo(payload);
	}

	@Test
	void websocketTransportWithReactorResource() {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(RSocketServer.Transport.WEBSOCKET);
		ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
		resourceFactory.afterPropertiesSet();
		factory.setResourceFactory(resourceFactory);
		int specificPort = SocketUtils.findAvailableTcpPort(41000);
		factory.setPort(specificPort);
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = createRSocketWebSocketClient(false);
		String payload = "test payload";
		String response = this.requester.route("test").data(payload).retrieveMono(String.class).block(TIMEOUT);
		assertThat(response).isEqualTo(payload);
		assertThat(this.server.address().getPort()).isEqualTo(specificPort);
	}

	@Test
	void serverProcessors() {
		NettyRSocketServerFactory factory = getFactory();
		ServerRSocketFactoryProcessor[] processors = new ServerRSocketFactoryProcessor[2];
		for (int i = 0; i < processors.length; i++) {
			processors[i] = mock(ServerRSocketFactoryProcessor.class);
			given(processors[i].process(any(RSocketFactory.ServerRSocketFactory.class)))
					.will((invocation) -> invocation.getArgument(0));
		}
		factory.setSocketFactoryProcessors(Arrays.asList(processors));
		this.server = factory.create(new EchoRequestResponseAcceptor());
		InOrder ordered = inOrder((Object[]) processors);
		for (ServerRSocketFactoryProcessor processor : processors) {
			ordered.verify(processor).process(any(RSocketFactory.ServerRSocketFactory.class));
		}
	}

	@Test
	void tcpTransportBasicSslFromClassPath() {
		testBasicSslWithKeyStore("classpath:test.jks", "password", Transport.TCP);
	}

	@Test
	void tcpTransportBasicSslFromFileSystem() {
		testBasicSslWithKeyStore("src/test/resources/test.jks", "password", Transport.TCP);
	}

	@Test
	void websocketTransportBasicSslFromClassPath() {
		testBasicSslWithKeyStore("classpath:test.jks", "password", Transport.WEBSOCKET);
	}

	@Test
	void websocketTransportBasicSslFromFileSystem() {
		testBasicSslWithKeyStore("src/test/resources/test.jks", "password", Transport.WEBSOCKET);
	}

	private void testBasicSslWithKeyStore(String keyStore, String keyPassword, Transport transport) {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(transport);
		Ssl ssl = new Ssl();
		ssl.setKeyStore(keyStore);
		ssl.setKeyPassword(keyPassword);
		factory.setSsl(ssl);
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = (transport == Transport.TCP) ? createRSocketTcpClient(true)
				: createRSocketWebSocketClient(true);
		String payload = "test payload";
		Mono<String> responseMono = this.requester.route("test").data(payload).retrieveMono(String.class);
		StepVerifier.create(responseMono).expectNext(payload).verifyComplete();
	}

	@Test
	void tcpTransportSslRejectsInsecureClient() {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(Transport.TCP);
		Ssl ssl = new Ssl();
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		factory.setSsl(ssl);
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = createRSocketTcpClient(false);
		String payload = "test payload";
		Mono<String> responseMono = this.requester.route("test").data(payload).retrieveMono(String.class);
		StepVerifier.create(responseMono)
				.verifyErrorSatisfies((ex) -> assertThatExceptionOfType(ClosedChannelException.class));
	}

	@Test
	void websocketTransportSslRejectsInsecureClient() {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(Transport.WEBSOCKET);
		Ssl ssl = new Ssl();
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		factory.setSsl(ssl);
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		// For WebSocket, the SSL failure results in a hang on the initial connect call
		assertThatThrownBy(() -> createRSocketWebSocketClient(false)).isInstanceOf(IllegalStateException.class)
				.hasStackTraceContaining("Timeout on blocking read");
	}

	private RSocketRequester createRSocketTcpClient(boolean ssl) {
		TcpClient tcpClient = createTcpClient(ssl);
		return createRSocketRequesterBuilder().connect(TcpClientTransport.create(tcpClient)).block(TIMEOUT);
	}

	private RSocketRequester createRSocketWebSocketClient(boolean ssl) {
		TcpClient tcpClient = createTcpClient(ssl);
		return createRSocketRequesterBuilder().connect(WebsocketClientTransport.create(tcpClient)).block(TIMEOUT);
	}

	private TcpClient createTcpClient(boolean ssl) {
		Assertions.assertThat(this.server).isNotNull();
		InetSocketAddress address = this.server.address();
		TcpClient tcpClient = TcpClient.create().host(address.getHostName()).port(address.getPort());
		if (ssl) {
			SslContextBuilder builder = SslContextBuilder.forClient().sslProvider(SslProvider.JDK)
					.trustManager(InsecureTrustManagerFactory.INSTANCE);
			tcpClient = tcpClient.secure((spec) -> spec.sslContext(builder));
		}
		return tcpClient;
	}

	private RSocketRequester.Builder createRSocketRequesterBuilder() {
		RSocketStrategies strategies = RSocketStrategies.builder().decoder(StringDecoder.allMimeTypes())
				.encoder(CharSequenceEncoder.allMimeTypes())
				.dataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT)).build();
		return RSocketRequester.builder().rsocketStrategies(strategies);
	}

	static class EchoRequestResponseAcceptor implements SocketAcceptor {

		@Override
		public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket rSocket) {
			return Mono.just(new AbstractRSocket() {
				@Override
				public Mono<Payload> requestResponse(Payload payload) {
					return Mono.just(DefaultPayload.create(payload));
				}
			});
		}

	}

}
