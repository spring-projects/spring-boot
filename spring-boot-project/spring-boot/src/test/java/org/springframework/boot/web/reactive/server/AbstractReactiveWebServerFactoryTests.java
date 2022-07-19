/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.reactive.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.awaitility.Awaitility;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.NettyPipeline;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Base for testing classes that extends {@link AbstractReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 */
public abstract class AbstractReactiveWebServerFactoryTests {

	protected WebServer webServer;

	@AfterEach
	void tearDown() {
		if (this.webServer != null) {
			try {
				this.webServer.stop();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
	}

	protected abstract AbstractReactiveWebServerFactory getFactory();

	@Test
	void specificPort() throws Exception {
		AbstractReactiveWebServerFactory factory = getFactory();
		int specificPort = doWithRetry(() -> {
			factory.setPort(0);
			this.webServer = factory.getWebServer(new EchoHandler());
			this.webServer.start();
			return this.webServer.getPort();
		});
		Mono<String> result = getWebClient(this.webServer.getPort()).build().post().uri("/test")
				.contentType(MediaType.TEXT_PLAIN).body(BodyInserters.fromValue("Hello World")).retrieve()
				.bodyToMono(String.class);
		assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
		assertThat(this.webServer.getPort()).isEqualTo(specificPort);
	}

	@Test
	void portIsMinusOneWhenConnectionIsClosed() {
		AbstractReactiveWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		assertThat(this.webServer.getPort()).isGreaterThan(0);
		this.webServer.stop();
		assertThat(this.webServer.getPort()).isEqualTo(-1);
	}

	@Test
	void basicSslFromClassPath() {
		testBasicSslWithKeyStore("classpath:test.jks", "password");
	}

	@Test
	void basicSslFromFileSystem() {
		testBasicSslWithKeyStore("src/test/resources/test.jks", "password");

	}

	protected final void testBasicSslWithKeyStore(String keyStore, String keyPassword) {
		AbstractReactiveWebServerFactory factory = getFactory();
		Ssl ssl = new Ssl();
		ssl.setKeyStore(keyStore);
		ssl.setKeyPassword(keyPassword);
		ssl.setKeyStorePassword("secret");
		factory.setSsl(ssl);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		ReactorClientHttpConnector connector = buildTrustAllSslConnector();
		WebClient client = WebClient.builder().baseUrl("https://localhost:" + this.webServer.getPort())
				.clientConnector(connector).build();
		Mono<String> result = client.post().uri("/test").contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromValue("Hello World")).retrieve().bodyToMono(String.class);
		assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
	}

	@Test
	void sslWithValidAlias() {
		String keyStore = "classpath:test.jks";
		String keyPassword = "password";
		AbstractReactiveWebServerFactory factory = getFactory();
		Ssl ssl = new Ssl();
		ssl.setKeyStore(keyStore);
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword(keyPassword);
		ssl.setKeyAlias("test-alias");
		factory.setSsl(ssl);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		ReactorClientHttpConnector connector = buildTrustAllSslConnector();
		WebClient client = WebClient.builder().baseUrl("https://localhost:" + this.webServer.getPort())
				.clientConnector(connector).build();

		Mono<String> result = client.post().uri("/test").contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromValue("Hello World")).retrieve().bodyToMono(String.class);

		StepVerifier.setDefaultTimeout(Duration.ofSeconds(30));
		StepVerifier.create(result).expectNext("Hello World").verifyComplete();
	}

	@Test
	void sslWithInvalidAliasFailsDuringStartup() {
		String keyStore = "classpath:test.jks";
		String keyPassword = "password";
		AbstractReactiveWebServerFactory factory = getFactory();
		Ssl ssl = new Ssl();
		ssl.setKeyStore(keyStore);
		ssl.setKeyPassword(keyPassword);
		ssl.setKeyAlias("test-alias-404");
		factory.setSsl(ssl);
		assertThatSslWithInvalidAliasCallFails(() -> factory.getWebServer(new EchoHandler()).start());
	}

	protected void assertThatSslWithInvalidAliasCallFails(ThrowingCallable call) {
		assertThatThrownBy(call).hasStackTraceContaining("Keystore does not contain specified alias 'test-alias-404'");
	}

	protected ReactorClientHttpConnector buildTrustAllSslConnector() {
		Http11SslContextSpec sslContextSpec = Http11SslContextSpec.forClient().configure(
				(builder) -> builder.sslProvider(SslProvider.JDK).trustManager(InsecureTrustManagerFactory.INSTANCE));
		HttpClient client = HttpClient.create().wiretap(true).secure((spec) -> spec.sslContext(sslContextSpec));
		return new ReactorClientHttpConnector(client);
	}

	@Test
	void sslWantsClientAuthenticationSucceedsWithClientCertificate() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(Ssl.ClientAuth.WANT);
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		ssl.setKeyStorePassword("secret");
		ssl.setTrustStore("classpath:test.jks");
		testClientAuthSuccess(ssl, buildTrustAllSslWithClientKeyConnector("test.jks", "password"));
	}

	@Test
	void sslWantsClientAuthenticationSucceedsWithoutClientCertificate() {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(Ssl.ClientAuth.WANT);
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		ssl.setTrustStore("classpath:test.jks");
		ssl.setKeyStorePassword("secret");
		testClientAuthSuccess(ssl, buildTrustAllSslConnector());
	}

	protected ReactorClientHttpConnector buildTrustAllSslWithClientKeyConnector(String keyStoreFile,
			String keyStorePassword) throws Exception {
		KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		try (InputStream stream = new FileInputStream("src/test/resources/" + keyStoreFile)) {
			clientKeyStore.load(stream, "secret".toCharArray());
		}
		KeyManagerFactory clientKeyManagerFactory = KeyManagerFactory
				.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		clientKeyManagerFactory.init(clientKeyStore, keyStorePassword.toCharArray());

		Http11SslContextSpec sslContextSpec = Http11SslContextSpec.forClient()
				.configure((builder) -> builder.sslProvider(SslProvider.JDK)
						.trustManager(InsecureTrustManagerFactory.INSTANCE).keyManager(clientKeyManagerFactory));
		HttpClient client = HttpClient.create().wiretap(true).secure((spec) -> spec.sslContext(sslContextSpec));
		return new ReactorClientHttpConnector(client);
	}

	protected void testClientAuthSuccess(Ssl sslConfiguration, ReactorClientHttpConnector clientConnector) {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setSsl(sslConfiguration);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		WebClient client = WebClient.builder().baseUrl("https://localhost:" + this.webServer.getPort())
				.clientConnector(clientConnector).build();
		Mono<String> result = client.post().uri("/test").contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromValue("Hello World")).retrieve().bodyToMono(String.class);
		assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
	}

	@Test
	void sslNeedsClientAuthenticationSucceedsWithClientCertificate() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(Ssl.ClientAuth.NEED);
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setTrustStore("classpath:test.jks");
		testClientAuthSuccess(ssl, buildTrustAllSslWithClientKeyConnector("test.jks", "password"));
	}

	@Test
	void sslNeedsClientAuthenticationFailsWithoutClientCertificate() {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(Ssl.ClientAuth.NEED);
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setTrustStore("classpath:test.jks");
		testClientAuthFailure(ssl, buildTrustAllSslConnector());
	}

	@Test
	void sslWithPemCertificates() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(Ssl.ClientAuth.NEED);
		ssl.setCertificate("classpath:test-cert.pem");
		ssl.setCertificatePrivateKey("classpath:test-key.pem");
		ssl.setTrustCertificate("classpath:test-cert.pem");
		testClientAuthSuccess(ssl, buildTrustAllSslWithClientKeyConnector("test.p12", "secret"));
	}

	protected void testClientAuthFailure(Ssl sslConfiguration, ReactorClientHttpConnector clientConnector) {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setSsl(sslConfiguration);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		WebClient client = WebClient.builder().baseUrl("https://localhost:" + this.webServer.getPort())
				.clientConnector(clientConnector).build();
		Mono<String> result = client.post().uri("/test").contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromValue("Hello World")).retrieve().bodyToMono(String.class);
		StepVerifier.create(result).expectError(WebClientRequestException.class).verify(Duration.ofSeconds(10));
	}

	protected WebClient.Builder getWebClient(int port) {
		return getWebClient(HttpClient.create().wiretap(true), port);
	}

	protected WebClient.Builder getWebClient(HttpClient client, int port) {
		InetSocketAddress address = new InetSocketAddress(port);
		String baseUrl = "http://" + address.getHostString() + ":" + address.getPort();
		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client)).baseUrl(baseUrl);
	}

	@Test
	protected void compressionOfResponseToGetRequest() {
		WebClient client = prepareCompressionTest();
		ResponseEntity<Void> response = client.get().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
		assertResponseIsCompressed(response);
	}

	@Test
	protected void compressionOfResponseToPostRequest() {
		WebClient client = prepareCompressionTest();
		ResponseEntity<Void> response = client.post().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
		assertResponseIsCompressed(response);
	}

	@Test
	void noCompressionForSmallResponse() {
		Compression compression = new Compression();
		compression.setEnabled(true);
		compression.setMinResponseSize(DataSize.ofBytes(3001));
		WebClient client = prepareCompressionTest(compression);
		ResponseEntity<Void> response = client.get().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
		assertResponseIsNotCompressed(response);
	}

	@Test
	void noCompressionForMimeType() {
		Compression compression = new Compression();
		compression.setEnabled(true);
		compression.setMimeTypes(new String[] { "application/json" });
		WebClient client = prepareCompressionTest(compression);
		ResponseEntity<Void> response = client.get().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
		assertResponseIsNotCompressed(response);
	}

	@Test
	protected void noCompressionForUserAgent() {
		Compression compression = new Compression();
		compression.setEnabled(true);
		compression.setExcludedUserAgents(new String[] { "testUserAgent" });
		WebClient client = prepareCompressionTest(compression);
		ResponseEntity<Void> response = client.get().header("User-Agent", "testUserAgent").retrieve().toBodilessEntity()
				.block(Duration.ofSeconds(30));
		assertResponseIsNotCompressed(response);
	}

	@Test
	void noCompressionForResponseWithInvalidContentType() {
		Compression compression = new Compression();
		compression.setEnabled(true);
		compression.setMimeTypes(new String[] { "application/json" });
		WebClient client = prepareCompressionTest(compression, "test~plain");
		ResponseEntity<Void> response = client.get().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
		assertResponseIsNotCompressed(response);
	}

	@Test
	void whenSslIsEnabledAndNoKeyStoreIsConfiguredThenServerFailsToStart() {
		assertThatThrownBy(() -> testBasicSslWithKeyStore(null, null))
				.hasMessageContaining("Could not load key store 'null'");
	}

	@Test
	void whenThereAreNoInFlightRequestsShutDownGracefullyReturnsTrueBeforePeriodElapses() throws Exception {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
		this.webServer.shutDownGracefully(result::set);
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> GracefulShutdownResult.IDLE == result.get());
	}

	@Test
	void whenARequestRemainsInFlightThenShutDownGracefullyDoesNotInvokeCallbackUntilTheRequestCompletes()
			throws Exception {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingHandler blockingHandler = new BlockingHandler();
		this.webServer = factory.getWebServer(blockingHandler);
		this.webServer.start();
		Mono<ResponseEntity<Void>> request = getWebClient(this.webServer.getPort()).build().get().retrieve()
				.toBodilessEntity();
		AtomicReference<ResponseEntity<Void>> responseReference = new AtomicReference<>();
		CountDownLatch responseLatch = new CountDownLatch(1);
		request.subscribe((response) -> {
			responseReference.set(response);
			responseLatch.countDown();
		});
		blockingHandler.awaitQueue();
		AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
		this.webServer.shutDownGracefully(result::set);
		assertThat(responseReference.get()).isNull();
		blockingHandler.completeOne();
		assertThat(responseLatch.await(5, TimeUnit.SECONDS)).isTrue();
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> GracefulShutdownResult.IDLE == result.get());
	}

	@Test
	void givenAnInflightRequestWhenTheServerIsStoppedThenGracefulShutdownCallbackIsCalledWithRequestsActive()
			throws Exception {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingHandler blockingHandler = new BlockingHandler();
		this.webServer = factory.getWebServer(blockingHandler);
		this.webServer.start();
		Mono<ResponseEntity<Void>> request = getWebClient(this.webServer.getPort()).build().get().retrieve()
				.toBodilessEntity();
		AtomicReference<ResponseEntity<Void>> responseReference = new AtomicReference<>();
		CountDownLatch responseLatch = new CountDownLatch(1);
		request.subscribe((response) -> {
			responseReference.set(response);
			responseLatch.countDown();
		});
		blockingHandler.awaitQueue();
		AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
		this.webServer.shutDownGracefully(result::set);
		assertThat(responseReference.get()).isNull();
		try {
			this.webServer.stop();
		}
		catch (Exception ex) {
			// Continue
		}
		System.out.println("Stopped");
		Awaitility.await().atMost(Duration.ofSeconds(5))
				.until(() -> GracefulShutdownResult.REQUESTS_ACTIVE == result.get());
		blockingHandler.completeOne();
	}

	@Test
	void whenARequestIsActiveAfterGracefulShutdownEndsThenStopWillComplete() throws InterruptedException {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingHandler blockingHandler = new BlockingHandler();
		this.webServer = factory.getWebServer(blockingHandler);
		this.webServer.start();
		Mono<ResponseEntity<Void>> request = getWebClient(this.webServer.getPort()).build().get().retrieve()
				.toBodilessEntity();
		AtomicReference<ResponseEntity<Void>> responseReference = new AtomicReference<>();
		CountDownLatch responseLatch = new CountDownLatch(1);
		request.subscribe((response) -> {
			responseReference.set(response);
			responseLatch.countDown();
		});
		blockingHandler.awaitQueue();
		AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
		this.webServer.shutDownGracefully(result::set);
		this.webServer.stop();
		Awaitility.await().atMost(Duration.ofSeconds(30))
				.until(() -> GracefulShutdownResult.REQUESTS_ACTIVE == result.get());
		blockingHandler.completeOne();
	}

	@Test
	void whenARequestIsActiveThenStopWillComplete() throws InterruptedException {
		AbstractReactiveWebServerFactory factory = getFactory();
		BlockingHandler blockingHandler = new BlockingHandler();
		this.webServer = factory.getWebServer(blockingHandler);
		this.webServer.start();
		Mono<ResponseEntity<Void>> request = getWebClient(this.webServer.getPort()).build().get().retrieve()
				.toBodilessEntity();
		AtomicReference<ResponseEntity<Void>> responseReference = new AtomicReference<>();
		CountDownLatch responseLatch = new CountDownLatch(1);
		request.subscribe((response) -> {
			responseReference.set(response);
			responseLatch.countDown();
		});
		blockingHandler.awaitQueue();
		try {
			this.webServer.stop();
		}
		catch (Exception ex) {
			// Continue
		}
		blockingHandler.completeOne();
	}

	@Test
	protected void whenHttp2IsEnabledAndSslIsDisabledThenH2cCanBeUsed() throws Exception {
		AbstractReactiveWebServerFactory factory = getFactory();
		Http2 http2 = new Http2();
		http2.setEnabled(true);
		factory.setHttp2(http2);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		org.eclipse.jetty.client.HttpClient client = new org.eclipse.jetty.client.HttpClient(
				new HttpClientTransportOverHTTP2(new HTTP2Client()));
		client.start();
		try {
			ContentResponse response = client.POST("http://localhost:" + this.webServer.getPort())
					.body(new StringRequestContent("text/plain", "Hello World")).send();
			assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
			assertThat(response.getContentAsString()).isEqualTo("Hello World");
		}
		finally {
			client.stop();
		}
	}

	@Test
	protected void whenHttp2IsEnabledAndSslIsDisabledThenHttp11CanStillBeUsed() {
		AbstractReactiveWebServerFactory factory = getFactory();
		Http2 http2 = new Http2();
		http2.setEnabled(true);
		factory.setHttp2(http2);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		Mono<String> result = getWebClient(this.webServer.getPort()).build().post().uri("/test")
				.contentType(MediaType.TEXT_PLAIN).body(BodyInserters.fromValue("Hello World")).retrieve()
				.bodyToMono(String.class);
		assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
	}

	protected WebClient prepareCompressionTest() {
		Compression compression = new Compression();
		compression.setEnabled(true);
		return prepareCompressionTest(compression);
	}

	protected WebClient prepareCompressionTest(Compression compression) {
		return prepareCompressionTest(compression, MediaType.TEXT_PLAIN_VALUE);
	}

	protected WebClient prepareCompressionTest(Compression compression, String responseContentType) {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setCompression(compression);
		this.webServer = factory.getWebServer(new CharsHandler(3000, responseContentType));
		this.webServer.start();

		HttpClient client = HttpClient.create().wiretap(true).compress(true)
				.doOnConnected((connection) -> connection.channel().pipeline().addBefore(NettyPipeline.HttpDecompressor,
						"CompressionTest", new CompressionDetectionHandler()));
		return getWebClient(client, this.webServer.getPort()).build();
	}

	protected void assertResponseIsCompressed(ResponseEntity<Void> response) {
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getFirst("X-Test-Compressed")).isEqualTo("true");
	}

	protected void assertResponseIsNotCompressed(ResponseEntity<Void> response) {
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().keySet()).doesNotContain("X-Test-Compressed");
	}

	protected void assertForwardHeaderIsUsed(AbstractReactiveWebServerFactory factory) {
		this.webServer = factory.getWebServer(new XForwardedHandler());
		this.webServer.start();
		String body = getWebClient(this.webServer.getPort()).build().get().header("X-Forwarded-Proto", "https")
				.retrieve().bodyToMono(String.class).block(Duration.ofSeconds(30));
		assertThat(body).isEqualTo("https");
	}

	private <T> T doWithRetry(Callable<T> action) throws Exception {
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

	protected final void doWithBlockedPort(BlockedPortAction action) throws Exception {
		ServerSocket serverSocket = new ServerSocket();
		int blockedPort = doWithRetry(() -> {
			serverSocket.bind(null);
			return serverSocket.getLocalPort();
		});
		try {
			action.run(blockedPort);
		}
		finally {
			serverSocket.close();
		}
	}

	public interface BlockedPortAction {

		void run(int port);

	}

	protected static class EchoHandler implements HttpHandler {

		public EchoHandler() {
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			response.setStatusCode(HttpStatus.OK);
			return response.writeWith(request.getBody());
		}

	}

	protected static class BlockingHandler implements HttpHandler {

		private final BlockingQueue<Sinks.Empty<Void>> processors = new ArrayBlockingQueue<>(10);

		private volatile boolean blocking = true;

		public BlockingHandler() {

		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			if (this.blocking) {
				Sinks.Empty<Void> completion = Sinks.empty();
				this.processors.add(completion);
				return completion.asMono().then(Mono.empty());
			}
			return Mono.empty();
		}

		public void completeOne() {
			try {
				Sinks.Empty<Void> processor = this.processors.take();
				processor.tryEmitEmpty();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		public void awaitQueue() throws InterruptedException {
			while (this.processors.isEmpty()) {
				Thread.sleep(100);
			}
		}

		public void stopBlocking() {
			this.blocking = false;
			this.processors.forEach(Sinks.Empty::tryEmitEmpty);
		}

	}

	static class CompressionDetectionHandler extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			if (msg instanceof HttpResponse response) {
				boolean compressed = response.headers().contains(HttpHeaderNames.CONTENT_ENCODING, "gzip", true);
				if (compressed) {
					response.headers().set("X-Test-Compressed", "true");
				}
			}
			ctx.fireChannelRead(msg);
		}

	}

	static class CharsHandler implements HttpHandler {

		private static final DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

		private final DataBuffer bytes;

		private final String mediaType;

		CharsHandler(int contentSize, String mediaType) {
			char[] chars = new char[contentSize];
			Arrays.fill(chars, 'F');
			this.bytes = factory.wrap(new String(chars).getBytes(StandardCharsets.UTF_8));
			this.mediaType = mediaType;
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			response.setStatusCode(HttpStatus.OK);
			response.getHeaders().set(HttpHeaders.CONTENT_TYPE, this.mediaType);
			response.getHeaders().setContentLength(this.bytes.readableByteCount());
			return response.writeWith(Mono.just(this.bytes));
		}

	}

	static class XForwardedHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			String scheme = request.getURI().getScheme();
			DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
			DataBuffer buffer = bufferFactory.wrap(scheme.getBytes(StandardCharsets.UTF_8));
			return response.writeWith(Mono.just(buffer));
		}

	}

}
