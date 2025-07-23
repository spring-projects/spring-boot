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

package org.springframework.boot.reactor.netty;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Arrays;

import io.netty.channel.Channel;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableChannel;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.reactive.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.AbstractReactiveWebServerFactoryTests;
import org.springframework.boot.web.server.reactive.ConfigurableReactiveWebServerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NettyReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 * @author Chris Bono
 * @author Moritz Halbritter
 */
class NettyReactiveWebServerFactoryTests extends AbstractReactiveWebServerFactoryTests {

	@Test
	void exceptionIsThrownWhenPortIsAlreadyInUse() {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setPort(0);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		factory.setPort(this.webServer.getPort());
		assertThatExceptionOfType(PortInUseException.class).isThrownBy(factory.getWebServer(new EchoHandler())::start)
			.satisfies(this::portMatchesRequirement)
			.withCauseInstanceOf(Throwable.class);
	}

	@Test
	void getPortWhenDisposableServerPortOperationIsUnsupportedReturnsMinusOne() {
		NettyReactiveWebServerFactory factory = new NoPortNettyReactiveWebServerFactory(0);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		assertThat(this.webServer.getPort()).isEqualTo(-1);
	}

	@Test
	void resourceFactoryAndWebServerLifecycle() {
		NettyReactiveWebServerFactory factory = getFactory();
		factory.setPort(0);
		ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
		factory.setResourceFactory(resourceFactory);
		this.webServer = factory.getWebServer(new EchoHandler());
		assertThatNoException().isThrownBy(() -> {
			resourceFactory.start();
			this.webServer.start();
			this.webServer.stop();
			resourceFactory.stop();
			resourceFactory.start();
			this.webServer.start();
		});
	}

	private void portMatchesRequirement(PortInUseException exception) {
		assertThat(exception.getPort()).isEqualTo(this.webServer.getPort());
	}

	@Test
	void nettyCustomizers() {
		NettyReactiveWebServerFactory factory = getFactory();
		NettyServerCustomizer[] customizers = new NettyServerCustomizer[2];
		for (int i = 0; i < customizers.length; i++) {
			customizers[i] = mock(NettyServerCustomizer.class);
			given(customizers[i].apply(any(HttpServer.class))).will((invocation) -> invocation.getArgument(0));
		}
		factory.setServerCustomizers(Arrays.asList(customizers[0], customizers[1]));
		this.webServer = factory.getWebServer(new EchoHandler());
		InOrder ordered = inOrder((Object[]) customizers);
		for (NettyServerCustomizer customizer : customizers) {
			ordered.verify(customizer).apply(any(HttpServer.class));
		}
	}

	@Test
	void useForwardedHeaders() {
		NettyReactiveWebServerFactory factory = getFactory();
		factory.setUseForwardHeaders(true);
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	@WithPackageResources("test.jks")
	void whenSslIsConfiguredWithAValidAliasARequestSucceeds() {
		Mono<String> result = testSslWithAlias("test-alias");
		StepVerifier.create(result).expectNext("Hello World").expectComplete().verify(Duration.ofSeconds(30));
	}

	@Test
	@WithPackageResources({ "1.key", "1.crt", "2.key", "2.crt" })
	void whenSslBundleIsUpdatedThenSslIsReloaded() {
		DefaultSslBundleRegistry bundles = new DefaultSslBundleRegistry("bundle1", createSslBundle("1.key", "1.crt"));
		Mono<String> result = testSslWithBundle(bundles, "bundle1");
		StepVerifier.create(result).expectNext("Hello World").expectComplete().verify(Duration.ofSeconds(30));
		bundles.updateBundle("bundle1", createSslBundle("2.key", "2.crt"));
		Mono<String> result2 = executeSslRequest();
		StepVerifier.create(result2).expectNext("Hello World").expectComplete().verify(Duration.ofSeconds(30));
	}

	@Test
	void whenServerIsShuttingDownGracefullyThenNewConnectionsCannotBeMade() {
		NettyReactiveWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingHandler blockingHandler = new BlockingHandler();
		this.webServer = factory.getWebServer(blockingHandler);
		this.webServer.start();
		WebClient webClient = getWebClient(this.webServer.getPort()).build();
		this.webServer.shutDownGracefully((result) -> {
		});
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> {
			blockingHandler.stopBlocking();
			try {
				webClient.get().retrieve().toBodilessEntity().block();
				return false;
			}
			catch (RuntimeException ex) {
				return ex.getCause() instanceof ConnectException;
			}
		});
		this.webServer.stop();
	}

	@Override
	@Test
	@Disabled("Reactor Netty does not support multiple ports")
	protected void startedLogMessageWithMultiplePorts() {
	}

	private Mono<String> testSslWithAlias(String alias) {
		String keyStore = "classpath:test.jks";
		String keyPassword = "password";
		NettyReactiveWebServerFactory factory = getFactory();
		Ssl ssl = new Ssl();
		ssl.setKeyStore(keyStore);
		ssl.setKeyPassword(keyPassword);
		ssl.setKeyAlias(alias);
		factory.setSsl(ssl);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		return executeSslRequest();
	}

	private Mono<String> testSslWithBundle(SslBundles sslBundles, String bundle) {
		NettyReactiveWebServerFactory factory = getFactory();
		factory.setSslBundles(sslBundles);
		factory.setSsl(Ssl.forBundle(bundle));
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		return executeSslRequest();
	}

	private Mono<String> executeSslRequest() {
		ReactorClientHttpConnector connector = buildTrustAllSslConnector();
		WebClient client = WebClient.builder()
			.baseUrl("https://localhost:" + this.webServer.getPort())
			.clientConnector(connector)
			.build();
		return client.post()
			.uri("/test")
			.contentType(MediaType.TEXT_PLAIN)
			.body(BodyInserters.fromValue("Hello World"))
			.retrieve()
			.bodyToMono(String.class);
	}

	@Override
	protected NettyReactiveWebServerFactory getFactory() {
		return new NettyReactiveWebServerFactory(0);
	}

	@Override
	protected String startedLogMessage() {
		return ((NettyWebServer) this.webServer).getStartedLogMessage();
	}

	@Override
	protected void addConnector(int port, ConfigurableReactiveWebServerFactory factory) {
		throw new UnsupportedOperationException("Reactor Netty does not support multiple ports");
	}

	private static SslBundle createSslBundle(String key, String certificate) {
		return SslBundle.of(new PemSslStoreBundle(
				new PemSslStoreDetails(null, "classpath:" + certificate, "classpath:" + key), null));
	}

	static class NoPortNettyReactiveWebServerFactory extends NettyReactiveWebServerFactory {

		NoPortNettyReactiveWebServerFactory(int port) {
			super(port);
		}

		@Override
		NettyWebServer createNettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter,
				Duration lifecycleTimeout, Shutdown shutdown) {
			return new NoPortNettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, shutdown);
		}

	}

	static class NoPortNettyWebServer extends NettyWebServer {

		NoPortNettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter, Duration lifecycleTimeout,
				Shutdown shutdown) {
			super(httpServer, handlerAdapter, lifecycleTimeout, shutdown, null);
		}

		@Override
		DisposableServer startHttpServer() {
			return new NoPortDisposableServer(super.startHttpServer());
		}

	}

	static class NoPortDisposableServer implements DisposableServer {

		private final DisposableServer delegate;

		NoPortDisposableServer(DisposableServer delegate) {
			this.delegate = delegate;
		}

		@Override
		public SocketAddress address() {
			return this.delegate.address();
		}

		@Override
		public String host() {
			return this.delegate.host();
		}

		@Override
		public String path() {
			return this.delegate.path();
		}

		@Override
		public Channel channel() {
			return this.delegate.channel();
		}

		@Override
		public void dispose() {
			this.delegate.dispose();
		}

		@Override
		public void disposeNow() {
			this.delegate.disposeNow();
		}

		@Override
		public void disposeNow(Duration timeout) {
			this.delegate.disposeNow(timeout);
		}

		@Override
		public CoreSubscriber<Void> disposeSubscriber() {
			return this.delegate.disposeSubscriber();
		}

		@Override
		public boolean isDisposed() {
			return this.delegate.isDisposed();
		}

		@Override
		public Mono<Void> onDispose() {
			return this.delegate.onDispose();
		}

		@Override
		public DisposableChannel onDispose(Disposable onDispose) {
			return this.delegate.onDispose(onDispose);
		}

	}

}
