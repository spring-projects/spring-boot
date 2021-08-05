/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.web.embedded.undertow;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.servlet.ServletRegistration.Dynamic;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletContainer;
import org.apache.http.HttpResponse;
import org.apache.jasper.servlet.JspServlet;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.boot.testsupport.web.servlet.ExampleServlet;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactoryTests;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UndertowServletWebServerFactory}.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 */
class UndertowServletWebServerFactoryTests extends AbstractServletWebServerFactoryTests {

	@Override
	protected UndertowServletWebServerFactory getFactory() {
		return new UndertowServletWebServerFactory(0);
	}

	@AfterEach
	void awaitClosureOfSslRelatedInputStreams() {
		// https://issues.redhat.com/browse/UNDERTOW-1705
		File resource = new File(this.tempDir, "test.txt");
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> (!resource.isFile()) || resource.delete());
	}

	@Test
	void errorPage404() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/hello"));
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(), "/hello"));
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
		assertThat(getResponse(getLocalUrl("/not-found"))).isEqualTo("Hello World");
	}

	@Test
	void setNullBuilderCustomizersThrows() {
		UndertowServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setBuilderCustomizers(null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void addNullAddBuilderCustomizersThrows() {
		UndertowServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addBuilderCustomizers((UndertowBuilderCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void builderCustomizers() {
		UndertowServletWebServerFactory factory = getFactory();
		UndertowBuilderCustomizer[] customizers = new UndertowBuilderCustomizer[4];
		Arrays.setAll(customizers, (i) -> mock(UndertowBuilderCustomizer.class));
		factory.setBuilderCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addBuilderCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) customizers);
		for (UndertowBuilderCustomizer customizer : customizers) {
			ordered.verify(customizer).customize(any(Builder.class));
		}
	}

	@Test
	void setNullDeploymentInfoCustomizersThrows() {
		UndertowServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setDeploymentInfoCustomizers(null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void addNullAddDeploymentInfoCustomizersThrows() {
		UndertowServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addDeploymentInfoCustomizers((UndertowDeploymentInfoCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void deploymentInfo() {
		UndertowServletWebServerFactory factory = getFactory();
		UndertowDeploymentInfoCustomizer[] customizers = new UndertowDeploymentInfoCustomizer[4];
		Arrays.setAll(customizers, (i) -> mock(UndertowDeploymentInfoCustomizer.class));
		factory.setDeploymentInfoCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addDeploymentInfoCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) customizers);
		for (UndertowDeploymentInfoCustomizer customizer : customizers) {
			ordered.verify(customizer).customize(any(DeploymentInfo.class));
		}
	}

	@Test
	void basicSslClasspathKeyStore() throws Exception {
		testBasicSslWithKeyStore("classpath:test.jks");
	}

	@Test
	void defaultContextPath() {
		UndertowServletWebServerFactory factory = getFactory();
		final AtomicReference<String> contextPath = new AtomicReference<>();
		factory.addDeploymentInfoCustomizers((deploymentInfo) -> contextPath.set(deploymentInfo.getContextPath()));
		this.webServer = factory.getWebServer();
		assertThat(contextPath.get()).isEqualTo("/");
	}

	@Test
	void useForwardHeaders() throws Exception {
		UndertowServletWebServerFactory factory = getFactory();
		factory.setUseForwardHeaders(true);
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	void eachFactoryUsesADiscreteServletContainer() {
		assertThat(getServletContainerFromNewFactory()).isNotEqualTo(getServletContainerFromNewFactory());
	}

	@Test
	void accessLogCanBeEnabled() throws IOException, URISyntaxException {
		testAccessLog(null, null, "access_log.log");
	}

	@Test
	void accessLogCanBeCustomized() throws IOException, URISyntaxException {
		testAccessLog("my_access.", "logz", "my_access.logz");
	}

	@Test
	void whenServerIsShuttingDownGracefullyThenRequestsAreRejectedWithServiceUnavailable() throws Exception {
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
		AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
		this.webServer.shutDownGracefully(result::set);
		assertThat(result.get()).isNull();
		blockingServlet.admitOne();
		assertThat(request.get()).isInstanceOf(HttpResponse.class);
		Object rejectedResult = initiateGetRequest(port, "/").get();
		assertThat(rejectedResult).isInstanceOf(HttpResponse.class);
		assertThat(((HttpResponse) rejectedResult).getStatusLine().getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
		this.webServer.stop();
	}

	private void testAccessLog(String prefix, String suffix, String expectedFile)
			throws IOException, URISyntaxException {
		UndertowServletWebServerFactory factory = getFactory();
		factory.setAccessLogEnabled(true);
		factory.setAccessLogPrefix(prefix);
		factory.setAccessLogSuffix(suffix);
		File accessLogDirectory = this.tempDir;
		factory.setAccessLogDirectory(accessLogDirectory);
		assertThat(accessLogDirectory.listFiles()).isEmpty();
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(), "/hello"));
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
		File accessLog = new File(accessLogDirectory, expectedFile);
		awaitFile(accessLog);
		assertThat(accessLogDirectory.listFiles()).contains(accessLog);
	}

	@Override
	protected void addConnector(int port, AbstractServletWebServerFactory factory) {
		((UndertowServletWebServerFactory) factory)
				.addBuilderCustomizers((builder) -> builder.addHttpListener(port, "0.0.0.0"));
	}

	@Test
	void sslRestrictedProtocolsEmptyCipherFailure() {
		assertThatIOException()
				.isThrownBy(() -> testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.2" },
						new String[] { "TLS_EMPTY_RENEGOTIATION_INFO_SCSV" }))
				.isInstanceOfAny(SSLException.class, SSLHandshakeException.class, SocketException.class);
	}

	@Test
	void sslRestrictedProtocolsECDHETLS1Failure() {
		assertThatIOException()
				.isThrownBy(() -> testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1" },
						new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" }))
				.isInstanceOfAny(SSLException.class, SocketException.class);
	}

	@Test
	void sslRestrictedProtocolsECDHESuccess() throws Exception {
		testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.2" },
				new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" });
	}

	@Test
	void sslRestrictedProtocolsRSATLS12Success() throws Exception {
		testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.2" },
				new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA256" });
	}

	@Test
	void sslRestrictedProtocolsRSATLS11Failure() {
		assertThatIOException()
				.isThrownBy(() -> testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.1" },
						new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA256" }))
				.isInstanceOfAny(SSLException.class, SocketException.class);
	}

	@Override
	protected JspServlet getJspServlet() {
		return null; // Undertow does not support JSPs
	}

	private void awaitFile(File file) {
		Awaitility.waitAtMost(Duration.ofSeconds(10)).until(file::exists, is(true));
	}

	private ServletContainer getServletContainerFromNewFactory() {
		UndertowServletWebServer container = (UndertowServletWebServer) getFactory().getWebServer();
		try {
			return container.getDeploymentManager().getDeployment().getServletContainer();
		}
		finally {
			container.stop();
		}
	}

	@Override
	protected Map<String, String> getActualMimeMappings() {
		return ((UndertowServletWebServer) this.webServer).getDeploymentManager().getDeployment()
				.getMimeExtensionMappings();
	}

	@Override
	protected Charset getCharset(Locale locale) {
		DeploymentInfo info = ((UndertowServletWebServer) this.webServer).getDeploymentManager().getDeployment()
				.getDeploymentInfo();
		String charsetName = info.getLocaleCharsetMapping().get(locale.toString());
		return (charsetName != null) ? Charset.forName(charsetName) : null;
	}

	@Override
	protected void handleExceptionCausedByBlockedPortOnPrimaryConnector(RuntimeException ex, int blockedPort) {
		assertThat(ex).isInstanceOf(PortInUseException.class);
		assertThat(((PortInUseException) ex).getPort()).isEqualTo(blockedPort);
		Undertow undertow = (Undertow) ReflectionTestUtils.getField(this.webServer, "undertow");
		assertThat(undertow.getWorker()).isNull();
	}

	@Override
	protected void handleExceptionCausedByBlockedPortOnSecondaryConnector(RuntimeException ex, int blockedPort) {
		handleExceptionCausedByBlockedPortOnPrimaryConnector(ex, blockedPort);
	}

}
