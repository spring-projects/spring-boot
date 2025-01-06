/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.concurrent.Callable;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider.GenericSslContextSpec;
import reactor.netty.tcp.TcpClient;
import reactor.test.StepVerifier;

import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServer.Transport;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.boot.web.server.Ssl;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.will;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NettyRSocketServerFactory}
 *
 * @author Brian Clozel
 * @author Leo Li
 * @author Chris Bono
 * @author Scott Frederick
 */
class NettyRSocketServerFactoryTests {

	private NettyRSocketServer server;

	private RSocketRequester requester;

	@AfterEach
	void tearDown() {
		if (this.requester != null) {
			this.requester.rsocketClient().dispose();
		}
		if (this.server != null) {
			try {
				this.server.stop();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
	}

	private NettyRSocketServerFactory getFactory() {
		NettyRSocketServerFactory factory = new NettyRSocketServerFactory();
		factory.setPort(0);
		return factory;
	}

	@Test
	void specificPort() {
		NettyRSocketServerFactory factory = getFactory();
		int specificPort = doWithRetry(() -> {
			factory.setPort(0);
			this.server = factory.create(new EchoRequestResponseAcceptor());
			this.server.start();
			return this.server.address().getPort();
		});
		this.requester = createRSocketTcpClient();
		assertThat(this.server.address().getPort()).isEqualTo(specificPort);
		checkEchoRequest();
	}

	@Test
	void websocketTransport() {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(RSocketServer.Transport.WEBSOCKET);
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = createRSocketWebSocketClient();
		checkEchoRequest();
	}

	@Test
	void websocketTransportWithReactorResource() {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(RSocketServer.Transport.WEBSOCKET);
		ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
		resourceFactory.afterPropertiesSet();
		factory.setResourceFactory(resourceFactory);
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = createRSocketWebSocketClient();
		checkEchoRequest();
	}

	@Test
	void serverCustomizers() {
		NettyRSocketServerFactory factory = getFactory();
		RSocketServerCustomizer[] customizers = new RSocketServerCustomizer[2];
		for (int i = 0; i < customizers.length; i++) {
			customizers[i] = mock(RSocketServerCustomizer.class);
			will((invocation) -> invocation.getArgument(0)).given(customizers[i])
				.customize(any(io.rsocket.core.RSocketServer.class));
		}
		factory.setRSocketServerCustomizers(Arrays.asList(customizers));
		this.server = factory.create(new EchoRequestResponseAcceptor());
		InOrder ordered = inOrder((Object[]) customizers);
		for (RSocketServerCustomizer customizer : customizers) {
			ordered.verify(customizer).customize(any(io.rsocket.core.RSocketServer.class));
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

	@Test
	void tcpTransportBasicSslCertificateFromClassPath() {
		testBasicSslWithPemCertificate("classpath:test-cert.pem", "classpath:test-key.pem", "classpath:test-cert.pem",
				Transport.TCP);
	}

	@Test
	void tcpTransportBasicSslCertificateFromFileSystem() {
		testBasicSslWithPemCertificate("src/test/resources/test-cert.pem", "src/test/resources/test-key.pem",
				"src/test/resources/test-cert.pem", Transport.TCP);
	}

	@Test
	void websocketTransportBasicSslCertificateFromClassPath() {
		testBasicSslWithPemCertificate("classpath:test-cert.pem", "classpath:test-key.pem", "classpath:test-cert.pem",
				Transport.WEBSOCKET);
	}

	@Test
	void websocketTransportBasicSslCertificateFromFileSystem() {
		testBasicSslWithPemCertificate("src/test/resources/test-cert.pem", "src/test/resources/test-key.pem",
				"src/test/resources/test-cert.pem", Transport.WEBSOCKET);
	}

	@Test
	void tcpTransportBasicSslFromClassPathWithBundle() {
		testBasicSslWithKeyStoreFromBundle("classpath:test.jks", "password", Transport.TCP);
	}

	@Test
	void tcpTransportBasicSslFromFileSystemWithBundle() {
		testBasicSslWithKeyStoreFromBundle("src/test/resources/test.jks", "password", Transport.TCP);
	}

	@Test
	void websocketTransportBasicSslFromClassPathWithBundle() {
		testBasicSslWithKeyStoreFromBundle("classpath:test.jks", "password", Transport.WEBSOCKET);
	}

	@Test
	void websocketTransportBasicSslFromFileSystemWithBundle() {
		testBasicSslWithKeyStoreFromBundle("src/test/resources/test.jks", "password", Transport.WEBSOCKET);
	}

	@Test
	void tcpTransportBasicSslCertificateFromClassPathWithBundle() {
		testBasicSslWithPemCertificateFromBundle("classpath:test-cert.pem", "classpath:test-key.pem",
				"classpath:test-cert.pem", Transport.TCP);
	}

	@Test
	void tcpTransportBasicSslCertificateFromFileSystemWithBundle() {
		testBasicSslWithPemCertificateFromBundle("src/test/resources/test-cert.pem", "src/test/resources/test-key.pem",
				"src/test/resources/test-cert.pem", Transport.TCP);
	}

	@Test
	void websocketTransportBasicSslCertificateFromClassPathWithBundle() {
		testBasicSslWithPemCertificateFromBundle("classpath:test-cert.pem", "classpath:test-key.pem",
				"classpath:test-cert.pem", Transport.WEBSOCKET);
	}

	@Test
	void websocketTransportBasicSslCertificateFromFileSystemWithBundle() {
		testBasicSslWithPemCertificateFromBundle("src/test/resources/test-cert.pem", "src/test/resources/test-key.pem",
				"src/test/resources/test-cert.pem", Transport.WEBSOCKET);
	}

	private void checkEchoRequest() {
		String payload = "test payload";
		Mono<String> response = this.requester.route("test").data(payload).retrieveMono(String.class);
		StepVerifier.create(response).expectNext(payload).expectComplete().verify(Duration.ofSeconds(30));
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
		this.requester = (transport == Transport.TCP) ? createSecureRSocketTcpClient()
				: createSecureRSocketWebSocketClient();
		checkEchoRequest();
	}

	private void testBasicSslWithPemCertificate(String certificate, String certificatePrivateKey,
			String trustCertificate, Transport transport) {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(transport);
		Ssl ssl = new Ssl();
		ssl.setCertificate(certificate);
		ssl.setCertificatePrivateKey(certificatePrivateKey);
		ssl.setTrustCertificate(trustCertificate);
		ssl.setKeyStorePassword("");
		factory.setSsl(ssl);
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = (transport == Transport.TCP) ? createSecureRSocketTcpClient()
				: createSecureRSocketWebSocketClient();
		checkEchoRequest();
	}

	private void testBasicSslWithKeyStoreFromBundle(String keyStore, String keyPassword, Transport transport) {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(transport);
		JksSslStoreDetails keyStoreDetails = JksSslStoreDetails.forLocation(keyStore);
		JksSslStoreDetails trustStoreDetails = null;
		SslBundle sslBundle = SslBundle.of(new JksSslStoreBundle(keyStoreDetails, trustStoreDetails),
				SslBundleKey.of(keyPassword));
		factory.setSsl(Ssl.forBundle("test"));
		factory.setSslBundles(new DefaultSslBundleRegistry("test", sslBundle));
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = (transport == Transport.TCP) ? createSecureRSocketTcpClient()
				: createSecureRSocketWebSocketClient();
		checkEchoRequest();
	}

	private void testBasicSslWithPemCertificateFromBundle(String certificate, String certificatePrivateKey,
			String trustCertificate, Transport transport) {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(transport);
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate(certificate)
			.withPrivateKey(certificatePrivateKey);
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate(trustCertificate);
		SslBundle sslBundle = SslBundle.of(new PemSslStoreBundle(keyStoreDetails, trustStoreDetails));
		factory.setSsl(Ssl.forBundle("test"));
		factory.setSslBundles(new DefaultSslBundleRegistry("test", sslBundle));
		this.server = factory.create(new EchoRequestResponseAcceptor());
		this.server.start();
		this.requester = (transport == Transport.TCP) ? createSecureRSocketTcpClient()
				: createSecureRSocketWebSocketClient();
		checkEchoRequest();
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
		this.requester = createRSocketTcpClient();
		String payload = "test payload";
		Mono<String> responseMono = this.requester.route("test").data(payload).retrieveMono(String.class);
		StepVerifier.create(responseMono)
			.verifyErrorSatisfies((ex) -> assertThat(ex).isInstanceOf(ClosedChannelException.class));
	}

	private RSocketRequester createRSocketTcpClient() {
		return createRSocketRequesterBuilder().transport(TcpClientTransport.create(createTcpClient()));
	}

	private RSocketRequester createRSocketWebSocketClient() {
		return createRSocketRequesterBuilder().transport(WebsocketClientTransport.create(createHttpClient(), "/"));
	}

	private RSocketRequester createSecureRSocketTcpClient() {
		return createRSocketRequesterBuilder().transport(TcpClientTransport.create(createSecureTcpClient()));
	}

	private RSocketRequester createSecureRSocketWebSocketClient() {
		return createRSocketRequesterBuilder()
			.transport(WebsocketClientTransport.create(createSecureHttpClient(), "/"));
	}

	private HttpClient createSecureHttpClient() {
		HttpClient httpClient = createHttpClient();
		GenericSslContextSpec<?> sslContextSpec = Http11SslContextSpec.forClient()
			.configure((builder) -> builder.sslProvider(SslProvider.JDK)
				.trustManager(InsecureTrustManagerFactory.INSTANCE));
		return httpClient.secure((spec) -> spec.sslContext(sslContextSpec));
	}

	private HttpClient createHttpClient() {
		Assertions.assertThat(this.server).isNotNull();
		InetSocketAddress address = this.server.address();
		return HttpClient.create().host(address.getHostName()).port(address.getPort());
	}

	private TcpClient createSecureTcpClient() {
		TcpClient tcpClient = createTcpClient();
		GenericSslContextSpec<?> sslContextSpec = Http11SslContextSpec.forClient()
			.configure((builder) -> builder.sslProvider(SslProvider.JDK)
				.trustManager(InsecureTrustManagerFactory.INSTANCE));
		return tcpClient.secure((spec) -> spec.sslContext(sslContextSpec));
	}

	private TcpClient createTcpClient() {
		Assertions.assertThat(this.server).isNotNull();
		InetSocketAddress address = this.server.address();
		return TcpClient.create().host(address.getHostName()).port(address.getPort());
	}

	private RSocketRequester.Builder createRSocketRequesterBuilder() {
		RSocketStrategies strategies = RSocketStrategies.builder()
			.decoder(StringDecoder.allMimeTypes())
			.encoder(CharSequenceEncoder.allMimeTypes())
			.dataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT))
			.build();
		return RSocketRequester.builder().rsocketStrategies(strategies);
	}

	private <T> T doWithRetry(Callable<T> action) {
		Exception lastFailure = null;
		for (int i = 0; i < 10; i++) {
			try {
				return action.call();
			}
			catch (Exception ex) {
				lastFailure = ex;
			}
		}
		throw new IllegalStateException("Action was not successful in 10 attempts", lastFailure);
	}

	static class EchoRequestResponseAcceptor implements SocketAcceptor {

		@Override
		public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket rSocket) {
			return Mono.just(new RSocket() {

				@Override
				public Mono<Payload> requestResponse(Payload payload) {
					return Mono.just(DefaultPayload.create(payload));
				}

			});
		}

	}

}
