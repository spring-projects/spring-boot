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

package org.springframework.boot.web.reactive.server;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.SocketUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for testing classes that extends {@link AbstractReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 */
public abstract class AbstractReactiveWebServerFactoryTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	protected WebServer webServer;

	@After
	public void tearDown() {
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
	public void specificPort() {
		AbstractReactiveWebServerFactory factory = getFactory();
		int specificPort = SocketUtils.findAvailableTcpPort(41000);
		factory.setPort(specificPort);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		Mono<String> result = getWebClient().build().post().uri("/test")
				.contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromObject("Hello World")).exchange()
				.flatMap((response) -> response.bodyToMono(String.class));
		assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
		assertThat(this.webServer.getPort()).isEqualTo(specificPort);
	}

	@Test
	public void basicSslFromClassPath() {
		testBasicSslWithKeyStore("classpath:test.jks", "password");
	}

	@Test
	public void basicSslFromFileSystem() {
		testBasicSslWithKeyStore("src/test/resources/test.jks", "password");
	}

	protected final void testBasicSslWithKeyStore(String keyStore, String keyPassword) {
		AbstractReactiveWebServerFactory factory = getFactory();
		Ssl ssl = new Ssl();
		ssl.setKeyStore(keyStore);
		ssl.setKeyPassword(keyPassword);
		factory.setSsl(ssl);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		ReactorClientHttpConnector connector = buildTrustAllSslConnector();
		WebClient client = WebClient.builder()
				.baseUrl("https://localhost:" + this.webServer.getPort())
				.clientConnector(connector).build();
		Mono<String> result = client.post().uri("/test").contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromObject("Hello World")).exchange()
				.flatMap((response) -> response.bodyToMono(String.class));
		assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
	}

	protected ReactorClientHttpConnector buildTrustAllSslConnector() {
		SslContextBuilder builder = SslContextBuilder.forClient()
				.sslProvider(SslProvider.JDK)
				.trustManager(InsecureTrustManagerFactory.INSTANCE);
		HttpClient client = HttpClient.create().wiretap(true)
				.secure((sslContextSpec) -> sslContextSpec.sslContext(builder));
		return new ReactorClientHttpConnector(client);
	}

	@Test
	public void sslWantsClientAuthenticationSucceedsWithClientCertificate()
			throws Exception {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(Ssl.ClientAuth.WANT);
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		ssl.setTrustStore("classpath:test.jks");
		testClientAuthSuccess(ssl, buildTrustAllSslWithClientKeyConnector());
	}

	@Test
	public void sslWantsClientAuthenticationSucceedsWithoutClientCertificate() {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(Ssl.ClientAuth.WANT);
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		ssl.setTrustStore("classpath:test.jks");
		testClientAuthSuccess(ssl, buildTrustAllSslConnector());
	}

	protected ReactorClientHttpConnector buildTrustAllSslWithClientKeyConnector()
			throws Exception {
		KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		clientKeyStore.load(new FileInputStream(new File("src/test/resources/test.jks")),
				"secret".toCharArray());
		KeyManagerFactory clientKeyManagerFactory = KeyManagerFactory
				.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		clientKeyManagerFactory.init(clientKeyStore, "password".toCharArray());
		SslContextBuilder builder = SslContextBuilder.forClient()
				.sslProvider(SslProvider.JDK)
				.trustManager(InsecureTrustManagerFactory.INSTANCE)
				.keyManager(clientKeyManagerFactory);
		HttpClient client = HttpClient.create().wiretap(true)
				.secure((sslContextSpec) -> sslContextSpec.sslContext(builder));
		return new ReactorClientHttpConnector(client);
	}

	protected void testClientAuthSuccess(Ssl sslConfiguration,
			ReactorClientHttpConnector clientConnector) {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setSsl(sslConfiguration);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		WebClient client = WebClient.builder()
				.baseUrl("https://localhost:" + this.webServer.getPort())
				.clientConnector(clientConnector).build();
		Mono<String> result = client.post().uri("/test").contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromObject("Hello World")).exchange()
				.flatMap((response) -> response.bodyToMono(String.class));
		assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
	}

	@Test
	public void sslNeedsClientAuthenticationSucceedsWithClientCertificate()
			throws Exception {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(Ssl.ClientAuth.NEED);
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		ssl.setTrustStore("classpath:test.jks");
		testClientAuthSuccess(ssl, buildTrustAllSslWithClientKeyConnector());
	}

	@Test
	public void sslNeedsClientAuthenticationFailsWithoutClientCertificate() {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(Ssl.ClientAuth.NEED);
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		ssl.setTrustStore("classpath:test.jks");
		testClientAuthFailure(ssl, buildTrustAllSslConnector());
	}

	protected void testClientAuthFailure(Ssl sslConfiguration,
			ReactorClientHttpConnector clientConnector) {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setSsl(sslConfiguration);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		WebClient client = WebClient.builder()
				.baseUrl("https://localhost:" + this.webServer.getPort())
				.clientConnector(clientConnector).build();
		Mono<String> result = client.post().uri("/test").contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromObject("Hello World")).exchange()
				.flatMap((response) -> response.bodyToMono(String.class));
		StepVerifier.create(result).expectError(SSLException.class)
				.verify(Duration.ofSeconds(10));
	}

	protected WebClient.Builder getWebClient() {
		return getWebClient(HttpClient.create().wiretap(true));
	}

	protected WebClient.Builder getWebClient(HttpClient client) {
		InetSocketAddress address = new InetSocketAddress(this.webServer.getPort());
		String baseUrl = "http://" + address.getHostString() + ":" + address.getPort();
		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client))
				.baseUrl(baseUrl);
	}

	@Test
	public void compressionOfResponseToGetRequest() {
		WebClient client = prepareCompressionTest();
		ResponseEntity<Void> response = client.get().exchange()
				.flatMap((res) -> res.toEntity(Void.class)).block(Duration.ofSeconds(30));
		assertResponseIsCompressed(response);
	}

	@Test
	public void compressionOfResponseToPostRequest() {
		WebClient client = prepareCompressionTest();
		ResponseEntity<Void> response = client.post().exchange()
				.flatMap((res) -> res.toEntity(Void.class)).block(Duration.ofSeconds(30));
		assertResponseIsCompressed(response);
	}

	@Test
	public void noCompressionForSmallResponse() {
		Compression compression = new Compression();
		compression.setEnabled(true);
		compression.setMinResponseSize(DataSize.ofBytes(3001));
		WebClient client = prepareCompressionTest(compression);
		ResponseEntity<Void> response = client.get().exchange()
				.flatMap((res) -> res.toEntity(Void.class)).block(Duration.ofSeconds(30));
		assertResponseIsNotCompressed(response);
	}

	@Test
	public void noCompressionForMimeType() {
		Compression compression = new Compression();
		compression.setMimeTypes(new String[] { "application/json" });
		WebClient client = prepareCompressionTest(compression);
		ResponseEntity<Void> response = client.get().exchange()
				.flatMap((res) -> res.toEntity(Void.class)).block(Duration.ofSeconds(30));
		assertResponseIsNotCompressed(response);
	}

	@Test
	public void noCompressionForUserAgent() {
		Compression compression = new Compression();
		compression.setEnabled(true);
		compression.setExcludedUserAgents(new String[] { "testUserAgent" });
		WebClient client = prepareCompressionTest(compression);
		ResponseEntity<Void> response = client.get().header("User-Agent", "testUserAgent")
				.exchange().flatMap((res) -> res.toEntity(Void.class))
				.block(Duration.ofSeconds(30));
		assertResponseIsNotCompressed(response);
	}

	protected WebClient prepareCompressionTest() {
		Compression compression = new Compression();
		compression.setEnabled(true);
		return prepareCompressionTest(compression);

	}

	protected WebClient prepareCompressionTest(Compression compression) {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setCompression(compression);
		this.webServer = factory
				.getWebServer(new CharsHandler(3000, MediaType.TEXT_PLAIN));
		this.webServer.start();

		HttpClient client = HttpClient.create().wiretap(true).compress(true)
				.tcpConfiguration((tcpClient) -> tcpClient.doOnConnected(
						(connection) -> connection.channel().pipeline().addBefore(
								NettyPipeline.HttpDecompressor, "CompressionTest",
								new CompressionDetectionHandler())));
		return getWebClient(client).build();
	}

	protected void assertResponseIsCompressed(ResponseEntity<Void> response) {
		assertThat(response.getHeaders().getFirst("X-Test-Compressed")).isEqualTo("true");
	}

	protected void assertResponseIsNotCompressed(ResponseEntity<Void> response) {
		assertThat(response.getHeaders().keySet()).doesNotContain("X-Test-Compressed");
	}

	protected void assertForwardHeaderIsUsed(AbstractReactiveWebServerFactory factory) {
		this.webServer = factory.getWebServer(new XForwardedHandler());
		this.webServer.start();
		String body = getWebClient().build().get().header("X-Forwarded-Proto", "https")
				.retrieve().bodyToMono(String.class).block(Duration.ofSeconds(30));
		assertThat(body).isEqualTo("https");
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

	protected static class CompressionDetectionHandler
			extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			if (msg instanceof HttpResponse) {
				HttpResponse response = (HttpResponse) msg;
				boolean compressed = response.headers()
						.contains(HttpHeaderNames.CONTENT_ENCODING, "gzip", true);
				if (compressed) {
					response.headers().set("X-Test-Compressed", "true");
				}
			}
			ctx.fireChannelRead(msg);
		}

	}

	protected static class CharsHandler implements HttpHandler {

		private static final DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

		private final DataBuffer bytes;

		private final MediaType mediaType;

		public CharsHandler(int contentSize, MediaType mediaType) {
			char[] chars = new char[contentSize];
			Arrays.fill(chars, 'F');
			this.bytes = factory.wrap(new String(chars).getBytes(StandardCharsets.UTF_8));
			this.mediaType = mediaType;
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			response.setStatusCode(HttpStatus.OK);
			response.getHeaders().setContentType(this.mediaType);
			response.getHeaders().setContentLength(this.bytes.readableByteCount());
			return response.writeWith(Mono.just(this.bytes));
		}

	}

	protected static class XForwardedHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			String scheme = request.getURI().getScheme();
			DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
			DataBuffer buffer = bufferFactory
					.wrap(scheme.getBytes(StandardCharsets.UTF_8));
			return response.writeWith(Mono.just(buffer));
		}

	}

}
