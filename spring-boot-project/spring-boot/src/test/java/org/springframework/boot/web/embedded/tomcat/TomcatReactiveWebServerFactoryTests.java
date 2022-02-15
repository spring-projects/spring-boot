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

package org.springframework.boot.web.embedded.tomcat;

import java.net.ConnectException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 * @author HaiTao Zhang
 */
class TomcatReactiveWebServerFactoryTests extends AbstractReactiveWebServerFactoryTests {

	@Override
	protected TomcatReactiveWebServerFactory getFactory() {
		return new TomcatReactiveWebServerFactory(0);
	}

	@Test
	void tomcatCustomizers() {
		TomcatReactiveWebServerFactory factory = getFactory();
		TomcatContextCustomizer[] customizers = new TomcatContextCustomizer[4];
		Arrays.setAll(customizers, (i) -> mock(TomcatContextCustomizer.class));
		factory.setTomcatContextCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addContextCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer(mock(HttpHandler.class));
		InOrder ordered = inOrder((Object[]) customizers);
		for (TomcatContextCustomizer customizer : customizers) {
			then(customizer).should(ordered).customize(any(Context.class));
		}
	}

	@Test
	void contextIsAddedToHostBeforeCustomizersAreCalled() {
		TomcatReactiveWebServerFactory factory = getFactory();
		TomcatContextCustomizer customizer = mock(TomcatContextCustomizer.class);
		factory.addContextCustomizers(customizer);
		this.webServer = factory.getWebServer(mock(HttpHandler.class));
		ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
		then(customizer).should().customize(contextCaptor.capture());
		assertThat(contextCaptor.getValue().getParent()).isNotNull();
	}

	@Test
	void defaultTomcatListeners() {
		TomcatReactiveWebServerFactory factory = getFactory();
		if (AprLifecycleListener.isAprAvailable()) {
			assertThat(factory.getContextLifecycleListeners()).hasSize(1).first()
					.isInstanceOf(AprLifecycleListener.class);
		}
		else {
			assertThat(factory.getContextLifecycleListeners()).isEmpty();
		}
	}

	@Test
	void tomcatListeners() {
		TomcatReactiveWebServerFactory factory = getFactory();
		LifecycleListener[] listeners = new LifecycleListener[4];
		Arrays.setAll(listeners, (i) -> mock(LifecycleListener.class));
		factory.setContextLifecycleListeners(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextLifecycleListeners(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer(mock(HttpHandler.class));
		InOrder ordered = inOrder((Object[]) listeners);
		for (LifecycleListener listener : listeners) {
			then(listener).should(ordered).lifecycleEvent(any(LifecycleEvent.class));
		}
	}

	@Test
	void setNullConnectorCustomizersShouldThrowException() {
		TomcatReactiveWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setTomcatConnectorCustomizers(null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void addNullAddConnectorCustomizersShouldThrowException() {
		TomcatReactiveWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addConnectorCustomizers((TomcatConnectorCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void setNullProtocolHandlerCustomizersShouldThrowException() {
		TomcatReactiveWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setTomcatProtocolHandlerCustomizers(null))
				.withMessageContaining("TomcatProtocolHandlerCustomizers must not be null");
	}

	@Test
	void addNullProtocolHandlerCustomizersShouldThrowException() {
		TomcatReactiveWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addProtocolHandlerCustomizers((TomcatProtocolHandlerCustomizer[]) null))
				.withMessageContaining("TomcatProtocolHandlerCustomizers must not be null");
	}

	@Test
	void tomcatConnectorCustomizersShouldBeInvoked() {
		TomcatReactiveWebServerFactory factory = getFactory();
		HttpHandler handler = mock(HttpHandler.class);
		TomcatConnectorCustomizer[] customizers = new TomcatConnectorCustomizer[4];
		Arrays.setAll(customizers, (i) -> mock(TomcatConnectorCustomizer.class));
		factory.setTomcatConnectorCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addConnectorCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer(handler);
		InOrder ordered = inOrder((Object[]) customizers);
		for (TomcatConnectorCustomizer customizer : customizers) {
			then(customizer).should(ordered).customize(any(Connector.class));
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void tomcatProtocolHandlerCustomizersShouldBeInvoked() {
		TomcatReactiveWebServerFactory factory = getFactory();
		HttpHandler handler = mock(HttpHandler.class);
		TomcatProtocolHandlerCustomizer<AbstractHttp11Protocol<?>>[] customizers = new TomcatProtocolHandlerCustomizer[4];
		Arrays.setAll(customizers, (i) -> mock(TomcatProtocolHandlerCustomizer.class));
		factory.setTomcatProtocolHandlerCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addProtocolHandlerCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer(handler);
		InOrder ordered = inOrder((Object[]) customizers);
		for (TomcatProtocolHandlerCustomizer customizer : customizers) {
			then(customizer).should(ordered).customize(any(ProtocolHandler.class));
		}
	}

	@Test
	void tomcatAdditionalConnectors() {
		TomcatReactiveWebServerFactory factory = getFactory();
		Connector[] connectors = new Connector[4];
		Arrays.setAll(connectors, (i) -> new Connector());
		factory.addAdditionalTomcatConnectors(connectors);
		this.webServer = factory.getWebServer(mock(HttpHandler.class));
		Map<Service, Connector[]> connectorsByService = ((TomcatWebServer) this.webServer).getServiceConnectors();
		assertThat(connectorsByService.values().iterator().next()).hasSize(connectors.length + 1);
	}

	@Test
	void addNullAdditionalConnectorsThrows() {
		TomcatReactiveWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.addAdditionalTomcatConnectors((Connector[]) null))
				.withMessageContaining("Connectors must not be null");
	}

	@Test
	void useForwardedHeaders() {
		TomcatReactiveWebServerFactory factory = getFactory();
		RemoteIpValve valve = new RemoteIpValve();
		valve.setProtocolHeader("X-Forwarded-Proto");
		factory.addEngineValves(valve);
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	void referenceClearingIsDisabled() {
		TomcatReactiveWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(mock(HttpHandler.class));
		this.webServer.start();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		StandardContext context = (StandardContext) tomcat.getHost().findChildren()[0];
		assertThat(context.getClearReferencesObjectStreamClassCaches()).isFalse();
		assertThat(context.getClearReferencesRmiTargets()).isFalse();
		assertThat(context.getClearReferencesThreadLocals()).isFalse();
	}

	@Test
	void portClashOfPrimaryConnectorResultsInPortInUseException() throws Exception {
		doWithBlockedPort((port) -> assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
			AbstractReactiveWebServerFactory factory = getFactory();
			factory.setPort(port);
			this.webServer = factory.getWebServer(mock(HttpHandler.class));
			this.webServer.start();
		}).satisfies((ex) -> handleExceptionCausedByBlockedPortOnPrimaryConnector(ex, port)));
	}

	@Override
	protected void assertThatSslWithInvalidAliasCallFails(ThrowingCallable call) {
		assertThatExceptionOfType(WebServerException.class).isThrownBy(call);
	}

	@Test
	void whenServerIsShuttingDownGracefullyThenNewConnectionsCannotBeMade() {
		TomcatReactiveWebServerFactory factory = getFactory();
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

	@Test
	void whenGetTomcatWebServerIsOverriddenThenWebServerCreationCanBeCustomized() {
		AtomicReference<TomcatWebServer> webServerReference = new AtomicReference<>();
		TomcatWebServer webServer = (TomcatWebServer) new TomcatReactiveWebServerFactory() {

			@Override
			protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
				webServerReference.set(new TomcatWebServer(tomcat));
				return webServerReference.get();
			}

		}.getWebServer(new EchoHandler());
		assertThat(webServerReference).hasValue(webServer);
	}

	private void handleExceptionCausedByBlockedPortOnPrimaryConnector(RuntimeException ex, int blockedPort) {
		assertThat(ex).isInstanceOf(PortInUseException.class);
		assertThat(((PortInUseException) ex).getPort()).isEqualTo(blockedPort);
	}

}
