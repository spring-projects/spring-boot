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

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.SessionIdGenerator;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.HttpClients;
import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactoryTests;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class TomcatServletWebServerFactoryTests extends AbstractServletWebServerFactoryTests {

	@Override
	protected TomcatServletWebServerFactory getFactory() {
		return new TomcatServletWebServerFactory(0);
	}

	@AfterEach
	void restoreTccl() {
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
	}

	// JMX MBean names clash if you get more than one Engine with the same name...
	@Test
	void tomcatEngineNames() {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		factory.setPort(0);
		TomcatWebServer tomcatWebServer = (TomcatWebServer) factory.getWebServer();
		// Make sure that the names are different
		String firstName = ((TomcatWebServer) this.webServer).getTomcat().getEngine().getName();
		String secondName = tomcatWebServer.getTomcat().getEngine().getName();
		assertThat(firstName).as("Tomcat engines must have different names").isNotEqualTo(secondName);
		tomcatWebServer.stop();
	}

	@Test
	void defaultTomcatListeners() {
		TomcatServletWebServerFactory factory = getFactory();
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
		TomcatServletWebServerFactory factory = getFactory();
		LifecycleListener[] listeners = new LifecycleListener[4];
		Arrays.setAll(listeners, (i) -> mock(LifecycleListener.class));
		factory.setContextLifecycleListeners(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextLifecycleListeners(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (LifecycleListener listener : listeners) {
			then(listener).should(ordered).lifecycleEvent(any(LifecycleEvent.class));
		}
	}

	@Test
	void tomcatCustomizers() {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatContextCustomizer[] customizers = new TomcatContextCustomizer[4];
		Arrays.setAll(customizers, (i) -> mock(TomcatContextCustomizer.class));
		factory.setTomcatContextCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addContextCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) customizers);
		for (TomcatContextCustomizer customizer : customizers) {
			then(customizer).should(ordered).customize(any(Context.class));
		}
	}

	@Test
	void contextIsAddedToHostBeforeCustomizersAreCalled() {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatContextCustomizer customizer = mock(TomcatContextCustomizer.class);
		factory.addContextCustomizers(customizer);
		this.webServer = factory.getWebServer();
		ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
		then(customizer).should().customize(contextCaptor.capture());
		assertThat(contextCaptor.getValue().getParent()).isNotNull();
	}

	@Test
	void tomcatConnectorCustomizers() {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatConnectorCustomizer[] customizers = new TomcatConnectorCustomizer[4];
		Arrays.setAll(customizers, (i) -> mock(TomcatConnectorCustomizer.class));
		factory.setTomcatConnectorCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addConnectorCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) customizers);
		for (TomcatConnectorCustomizer customizer : customizers) {
			then(customizer).should(ordered).customize(any(Connector.class));
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void tomcatProtocolHandlerCustomizersShouldBeInvoked() {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatProtocolHandlerCustomizer<AbstractHttp11Protocol<?>>[] customizers = new TomcatProtocolHandlerCustomizer[4];
		Arrays.setAll(customizers, (i) -> mock(TomcatProtocolHandlerCustomizer.class));
		factory.setTomcatProtocolHandlerCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addProtocolHandlerCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) customizers);
		for (TomcatProtocolHandlerCustomizer customizer : customizers) {
			then(customizer).should(ordered).customize(any(ProtocolHandler.class));
		}
	}

	@Test
	void tomcatProtocolHandlerCanBeCustomized() {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatProtocolHandlerCustomizer<AbstractHttp11Protocol<?>> customizer = (protocolHandler) -> protocolHandler
				.setProcessorCache(250);
		factory.addProtocolHandlerCustomizers(customizer);
		Tomcat tomcat = getTomcat(factory);
		Connector connector = ((TomcatWebServer) this.webServer).getServiceConnectors().get(tomcat.getService())[0];
		AbstractHttp11Protocol<?> protocolHandler = (AbstractHttp11Protocol<?>) connector.getProtocolHandler();
		assertThat(protocolHandler.getProcessorCache()).isEqualTo(250);
	}

	@Test
	void tomcatAdditionalConnectors() {
		TomcatServletWebServerFactory factory = getFactory();
		Connector[] connectors = new Connector[4];
		Arrays.setAll(connectors, (i) -> new Connector());
		factory.addAdditionalTomcatConnectors(connectors);
		this.webServer = factory.getWebServer();
		Map<Service, Connector[]> connectorsByService = ((TomcatWebServer) this.webServer).getServiceConnectors();
		assertThat(connectorsByService.values().iterator().next()).hasSize(connectors.length + 1);
	}

	@Test
	void addNullAdditionalConnectorThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.addAdditionalTomcatConnectors((Connector[]) null))
				.withMessageContaining("Connectors must not be null");
	}

	@Test
	void sessionTimeout() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.getSession().setTimeout(Duration.ofSeconds(10));
		assertTimeout(factory, 1);
	}

	@Test
	void sessionTimeoutInMinutes() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.getSession().setTimeout(Duration.ofMinutes(1));
		assertTimeout(factory, 1);
	}

	@Test
	void noSessionTimeout() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.getSession().setTimeout(null);
		assertTimeout(factory, -1);
	}

	@Test
	void valve() {
		TomcatServletWebServerFactory factory = getFactory();
		Valve valve = mock(Valve.class);
		factory.addContextValves(valve);
		this.webServer = factory.getWebServer();
		then(valve).should().setNext(any(Valve.class));
	}

	@Test
	void setNullTomcatContextCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setTomcatContextCustomizers(null))
				.withMessageContaining("TomcatContextCustomizers must not be null");
	}

	@Test
	void addNullContextCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addContextCustomizers((TomcatContextCustomizer[]) null))
				.withMessageContaining("TomcatContextCustomizers must not be null");
	}

	@Test
	void setNullTomcatConnectorCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setTomcatConnectorCustomizers(null))
				.withMessageContaining("TomcatConnectorCustomizers must not be null");
	}

	@Test
	void addNullConnectorCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addConnectorCustomizers((TomcatConnectorCustomizer[]) null))
				.withMessageContaining("TomcatConnectorCustomizers must not be null");
	}

	@Test
	void setNullTomcatProtocolHandlerCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setTomcatProtocolHandlerCustomizers(null))
				.withMessageContaining("TomcatProtocolHandlerCustomizers must not be null");
	}

	@Test
	void addNullTomcatProtocolHandlerCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addProtocolHandlerCustomizers((TomcatProtocolHandlerCustomizer[]) null))
				.withMessageContaining("TomcatProtocolHandlerCustomizers must not be null");
	}

	@Test
	void uriEncoding() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.setUriEncoding(StandardCharsets.US_ASCII);
		Tomcat tomcat = getTomcat(factory);
		Connector connector = ((TomcatWebServer) this.webServer).getServiceConnectors().get(tomcat.getService())[0];
		assertThat(connector.getURIEncoding()).isEqualTo("US-ASCII");
	}

	@Test
	void defaultUriEncoding() {
		TomcatServletWebServerFactory factory = getFactory();
		Tomcat tomcat = getTomcat(factory);
		Connector connector = ((TomcatWebServer) this.webServer).getServiceConnectors().get(tomcat.getService())[0];
		assertThat(connector.getURIEncoding()).isEqualTo("UTF-8");
	}

	@Test
	void startupFailureDoesNotResultInUnstoppedThreadsBeingReported(CapturedOutput output) throws Exception {
		super.portClashOfPrimaryConnectorResultsInPortInUseException();
		assertThat(output).doesNotContain("appears to have started a thread named [main]");
	}

	@Test
	void stopCalledWithoutStart() {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.stop();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		assertThat(tomcat.getServer().getState()).isSameAs(LifecycleState.DESTROYED);
	}

	@Override
	protected void addConnector(int port, AbstractServletWebServerFactory factory) {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(port);
		((TomcatServletWebServerFactory) factory).addAdditionalTomcatConnectors(connector);
	}

	@Test
	void useForwardHeaders() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		factory.addContextValves(new RemoteIpValve());
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	void disableDoesNotSaveSessionFiles() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		// If baseDir is not set SESSIONS.ser is written to a different temp directory
		// each time. By setting it we can really ensure that data isn't saved
		factory.setBaseDirectory(this.tempDir);
		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		String s1 = getResponse(getLocalUrl("/session"));
		String s2 = getResponse(getLocalUrl("/session"));
		this.webServer.stop();
		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		String s3 = getResponse(getLocalUrl("/session"));
		String message = "Session error s1=" + s1 + " s2=" + s2 + " s3=" + s3;
		assertThat(s2.split(":")[0]).as(message).isEqualTo(s1.split(":")[1]);
		assertThat(s3.split(":")[0]).as(message).isNotEqualTo(s2.split(":")[1]);
	}

	@Test
	void jndiLookupsCanBePerformedDuringApplicationContextRefresh() throws NamingException {
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0) {

			@Override
			protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
				tomcat.enableNaming();
				return super.getTomcatWebServer(tomcat);
			}

		};
		// Server is created in onRefresh
		this.webServer = factory.getWebServer();
		// Lookups should now be possible
		new InitialContext().lookup("java:comp/env");
		// Called in finishRefresh, giving us an opportunity to remove the context binding
		// and avoid a leak
		this.webServer.start();
		// Lookups should no longer be possible
		assertThatExceptionOfType(NamingException.class).isThrownBy(() -> new InitialContext().lookup("java:comp/env"));
	}

	@Test
	void defaultLocaleCharsetMappingsAreOverridden() throws IOException {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		// override defaults, see org.apache.catalina.util.CharsetMapperDefault.properties
		Properties charsetMapperDefault = PropertiesLoaderUtils
				.loadProperties(new ClassPathResource("CharsetMapperDefault.properties", CharsetMapper.class));
		for (String language : charsetMapperDefault.stringPropertyNames()) {
			assertThat(getCharset(new Locale(language))).isEqualTo(StandardCharsets.UTF_8);
		}
	}

	@Test
	void sessionIdGeneratorIsConfiguredWithAttributesFromTheManager() {
		System.setProperty("jvmRoute", "test");
		try {
			TomcatServletWebServerFactory factory = getFactory();
			this.webServer = factory.getWebServer();
			this.webServer.start();
		}
		finally {
			System.clearProperty("jvmRoute");
		}
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Context context = (Context) tomcat.getHost().findChildren()[0];
		SessionIdGenerator sessionIdGenerator = context.getManager().getSessionIdGenerator();
		assertThat(sessionIdGenerator).isInstanceOf(LazySessionIdGenerator.class);
		assertThat(sessionIdGenerator.getJvmRoute()).isEqualTo("test");
	}

	@Test
	void tldSkipPatternsShouldBeAppliedToContextJarScanner() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.addTldSkipPatterns("foo.jar", "bar.jar");
		this.webServer = factory.getWebServer();
		this.webServer.start();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Context context = (Context) tomcat.getHost().findChildren()[0];
		JarScanFilter jarScanFilter = context.getJarScanner().getJarScanFilter();
		assertThat(jarScanFilter.check(JarScanType.TLD, "foo.jar")).isFalse();
		assertThat(jarScanFilter.check(JarScanType.TLD, "bar.jar")).isFalse();
		assertThat(jarScanFilter.check(JarScanType.TLD, "test.jar")).isTrue();
	}

	@Test
	void tldScanPatternsShouldBeAppliedToContextJarScanner() {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		this.webServer.start();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Context context = (Context) tomcat.getHost().findChildren()[0];
		JarScanFilter jarScanFilter = context.getJarScanner().getJarScanFilter();
		String tldScan = ((StandardJarScanFilter) jarScanFilter).getTldScan();
		assertThat(tldScan).isEqualTo("log4j-taglib*.jar,log4j-web*.jar,log4javascript*.jar,slf4j-taglib*.jar");
	}

	@Test
	void customTomcatHttpOnlyCookie() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.getSession().getCookie().setHttpOnly(false);
		this.webServer = factory.getWebServer();
		this.webServer.start();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Context context = (Context) tomcat.getHost().findChildren()[0];
		assertThat(context.getUseHttpOnly()).isFalse();
	}

	@Test
	void exceptionThrownOnLoadFailureWhenFailCtxIfServletStartFailsIsTrue() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.addContextCustomizers((context) -> {
			if (context instanceof StandardContext) {
				((StandardContext) context).setFailCtxIfServletStartFails(true);
			}
		});
		this.webServer = factory
				.getWebServer((context) -> context.addServlet("failing", FailingServlet.class).setLoadOnStartup(0));
		assertThatExceptionOfType(WebServerException.class).isThrownBy(this.webServer::start);
	}

	@Test
	void exceptionThrownOnLoadFailureWhenFailCtxIfServletStartFailsIsFalse() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.addContextCustomizers((context) -> {
			if (context instanceof StandardContext) {
				((StandardContext) context).setFailCtxIfServletStartFails(false);
			}
		});
		this.webServer = factory
				.getWebServer((context) -> context.addServlet("failing", FailingServlet.class).setLoadOnStartup(0));
		this.webServer.start();
	}

	@Test
	void referenceClearingIsDisabled() {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		this.webServer.start();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		StandardContext context = (StandardContext) tomcat.getHost().findChildren()[0];
		assertThat(context.getClearReferencesObjectStreamClassCaches()).isFalse();
		assertThat(context.getClearReferencesRmiTargets()).isFalse();
		assertThat(context.getClearReferencesThreadLocals()).isFalse();
	}

	@Test
	void nonExistentUploadDirectoryIsCreatedUponMultipartUpload() throws IOException, URISyntaxException {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		AtomicReference<ServletContext> servletContextReference = new AtomicReference<>();
		factory.addInitializers((servletContext) -> {
			servletContextReference.set(servletContext);
			Dynamic servlet = servletContext.addServlet("upload", new HttpServlet() {

				@Override
				protected void doPost(HttpServletRequest req, HttpServletResponse resp)
						throws ServletException, IOException {
					req.getParts();
				}

			});
			servlet.addMapping("/upload");
			servlet.setMultipartConfig(new MultipartConfigElement((String) null));
		});
		this.webServer = factory.getWebServer();
		this.webServer.start();
		File temp = (File) servletContextReference.get().getAttribute(ServletContext.TEMPDIR);
		FileSystemUtils.deleteRecursively(temp);
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new ByteArrayResource(new byte[1024 * 1024]));
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(getLocalUrl("/upload"), requestEntity,
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void exceptionThrownOnContextListenerDestroysServer() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0) {

			@Override
			protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
				try {
					return super.getTomcatWebServer(tomcat);
				}
				finally {
					assertThat(tomcat.getServer().getState()).isEqualTo(LifecycleState.DESTROYED);
				}
			}

		};
		assertThatExceptionOfType(WebServerException.class).isThrownBy(
				() -> factory.getWebServer((context) -> context.addListener(new FailingServletContextListener())));
	}

	@Test
	void registerJspServletWithDefaultLoadOnStartup() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		factory.addInitializers((context) -> context.addServlet("manually-registered-jsp-servlet", JspServlet.class));
		this.webServer = factory.getWebServer();
		this.webServer.start();
	}

	@Override
	protected void assertThatSslWithInvalidAliasCallFails(ThrowingCallable call) {
		assertThatExceptionOfType(WebServerException.class).isThrownBy(call);
	}

	@Test
	void whenServerIsShuttingDownGracefullyThenNewConnectionsCannotBeMade() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingServlet blockingServlet = new BlockingServlet();
		this.webServer = factory.getWebServer((context) -> {
			Dynamic registration = context.addServlet("blockingServlet", blockingServlet);
			registration.addMapping("/blocking");
			registration.setAsyncSupported(true);
		});
		this.webServer.start();
		int port = this.webServer.getPort();
		Future<Object> request = initiateGetRequest(port, "/blocking");
		blockingServlet.awaitQueue();
		this.webServer.shutDownGracefully((result) -> {
		});
		Object unconnectableRequest = Awaitility.await().until(
				() -> initiateGetRequest(HttpClients.createDefault(), port, "/").get(),
				(result) -> result instanceof Exception);
		assertThat(unconnectableRequest).isInstanceOf(HttpHostConnectException.class);
		blockingServlet.admitOne();
		assertThat(request.get()).isInstanceOf(HttpResponse.class);
		this.webServer.stop();
	}

	@Test
	void whenServerIsShuttingDownARequestOnAnIdleConnectionResultsInConnectionReset() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingServlet blockingServlet = new BlockingServlet();
		this.webServer = factory.getWebServer((context) -> {
			Dynamic registration = context.addServlet("blockingServlet", blockingServlet);
			registration.addMapping("/blocking");
			registration.setAsyncSupported(true);
		});
		HttpClient httpClient = HttpClients.createMinimal();
		this.webServer.start();
		int port = this.webServer.getPort();
		Future<Object> keepAliveRequest = initiateGetRequest(httpClient, port, "/blocking");
		blockingServlet.awaitQueue();
		blockingServlet.admitOne();
		assertThat(keepAliveRequest.get()).isInstanceOf(HttpResponse.class);
		Future<Object> request = initiateGetRequest(port, "/blocking");
		blockingServlet.awaitQueue();
		this.webServer.shutDownGracefully((result) -> {
		});
		Object idleConnectionRequestResult = Awaitility.await().until(() -> {
			Future<Object> idleConnectionRequest = initiateGetRequest(httpClient, port, "/");
			Object result = idleConnectionRequest.get();
			return result;
		}, (result) -> result instanceof Exception);
		assertThat(idleConnectionRequestResult).isInstanceOfAny(SocketException.class, NoHttpResponseException.class);
		if (idleConnectionRequestResult instanceof SocketException) {
			assertThat((SocketException) idleConnectionRequestResult).hasMessage("Connection reset");
		}
		blockingServlet.admitOne();
		Object response = request.get();
		assertThat(response).isInstanceOf(HttpResponse.class);
		this.webServer.stop();
	}

	@Override
	protected JspServlet getJspServlet() throws ServletException {
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Container container = tomcat.getHost().findChildren()[0];
		StandardWrapper standardWrapper = (StandardWrapper) container.findChild("jsp");
		if (standardWrapper == null) {
			return null;
		}
		standardWrapper.load();
		return (JspServlet) standardWrapper.getServlet();
	}

	@Override
	protected Map<String, String> getActualMimeMappings() {
		Context context = (Context) ((TomcatWebServer) this.webServer).getTomcat().getHost().findChildren()[0];
		Map<String, String> mimeMappings = new HashMap<>();
		for (String extension : context.findMimeMappings()) {
			mimeMappings.put(extension, context.findMimeMapping(extension));
		}
		return mimeMappings;
	}

	@Override
	protected Charset getCharset(Locale locale) {
		Context context = (Context) ((TomcatWebServer) this.webServer).getTomcat().getHost().findChildren()[0];
		CharsetMapper mapper = ((TomcatEmbeddedContext) context).getCharsetMapper();
		String charsetName = mapper.getCharset(locale);
		return (charsetName != null) ? Charset.forName(charsetName) : null;
	}

	private void assertTimeout(TomcatServletWebServerFactory factory, int expected) {
		Tomcat tomcat = getTomcat(factory);
		Context context = (Context) tomcat.getHost().findChildren()[0];
		assertThat(context.getSessionTimeout()).isEqualTo(expected);
	}

	private Tomcat getTomcat(TomcatServletWebServerFactory factory) {
		this.webServer = factory.getWebServer();
		return ((TomcatWebServer) this.webServer).getTomcat();
	}

	@Override
	protected void handleExceptionCausedByBlockedPortOnPrimaryConnector(RuntimeException ex, int blockedPort) {
		assertThat(ex).isInstanceOf(PortInUseException.class);
		assertThat(((PortInUseException) ex).getPort()).isEqualTo(blockedPort);
	}

	@Override
	protected void handleExceptionCausedByBlockedPortOnSecondaryConnector(RuntimeException ex, int blockedPort) {
		assertThat(ex).isInstanceOf(ConnectorStartFailedException.class);
		assertThat(((ConnectorStartFailedException) ex).getPort()).isEqualTo(blockedPort);
	}

}
