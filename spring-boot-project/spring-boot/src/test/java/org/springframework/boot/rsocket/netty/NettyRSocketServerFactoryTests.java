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
import java.time.Duration;
import java.util.Arrays;

import io.netty.buffer.PooledByteBufAllocator;
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
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;

import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryCustomizer;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NettyRSocketServerFactory}
 *
 * @author Brian Clozel
 */
public class NettyRSocketServerFactoryTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private NettyRSocketServer rSocketServer;

	private RSocketRequester requester;

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	@After
	public void tearDown() {
		if (this.rSocketServer != null) {
			try {
				this.rSocketServer.stop();
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
	public void specificPort() {
		NettyRSocketServerFactory factory = getFactory();
		int specificPort = SocketUtils.findAvailableTcpPort(41000);
		factory.setPort(specificPort);
		this.rSocketServer = factory.create(new EchoRequestResponseAcceptor());
		this.rSocketServer.start();
		this.requester = getRSocketRequester(createRSocketTcpClient());
		String payload = "test payload";
		String response = this.requester.route("test").data(payload)
				.retrieveMono(String.class).block(TIMEOUT);

		assertThat(response).isEqualTo(payload);
		assertThat(this.rSocketServer.address().getPort()).isEqualTo(specificPort);
	}

	@Test
	public void websocketTransport() {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(RSocketServer.TRANSPORT.WEBSOCKET);
		this.rSocketServer = factory.create(new EchoRequestResponseAcceptor());
		this.rSocketServer.start();
		this.requester = getRSocketRequester(createRSocketWebSocketClient());
		String payload = "test payload";
		String response = this.requester.route("test").data(payload)
				.retrieveMono(String.class).block(TIMEOUT);
		assertThat(response).isEqualTo(payload);
	}

	@Test
	public void serverCustomizers() {
		NettyRSocketServerFactory factory = getFactory();
		ServerRSocketFactoryCustomizer[] customizers = new ServerRSocketFactoryCustomizer[2];
		for (int i = 0; i < customizers.length; i++) {
			customizers[i] = mock(ServerRSocketFactoryCustomizer.class);
			given(customizers[i].apply(any(RSocketFactory.ServerRSocketFactory.class)))
					.will((invocation) -> invocation.getArgument(0));
		}
		factory.setServerCustomizers(Arrays.asList(customizers[0], customizers[1]));
		this.rSocketServer = factory.create(new EchoRequestResponseAcceptor());
		InOrder ordered = inOrder((Object[]) customizers);
		for (ServerRSocketFactoryCustomizer customizer : customizers) {
			ordered.verify(customizer)
					.apply(any(RSocketFactory.ServerRSocketFactory.class));
		}
	}

	private RSocket createRSocketTcpClient() {
		Assertions.assertThat(this.rSocketServer).isNotNull();
		InetSocketAddress address = this.rSocketServer.address();
		return RSocketFactory.connect().dataMimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
				.transport(TcpClientTransport.create(address)).start().block();
	}

	private RSocket createRSocketWebSocketClient() {
		Assertions.assertThat(this.rSocketServer).isNotNull();
		InetSocketAddress address = this.rSocketServer.address();
		return RSocketFactory.connect().dataMimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
				.transport(WebsocketClientTransport.create(address)).start().block();
	}

	private RSocketRequester getRSocketRequester(RSocket rSocketClient) {
		RSocketStrategies strategies = RSocketStrategies.builder()
				.decoder(StringDecoder.allMimeTypes())
				.encoder(CharSequenceEncoder.allMimeTypes())
				.dataBufferFactory(
						new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT))
				.build();
		return RSocketRequester.create(rSocketClient, MimeTypeUtils.TEXT_PLAIN,
				strategies);
	}

	static class EchoRequestResponseAcceptor implements SocketAcceptor {

		@Override
		public Mono<RSocket> accept(ConnectionSetupPayload setupPayload,
				RSocket rSocket) {
			return Mono.just(new AbstractRSocket() {
				@Override
				public Mono<Payload> requestResponse(Payload payload) {
					return Mono.just(DefaultPayload.create(payload));
				}
			});
		}

	}

}
