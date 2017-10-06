/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.embedded.undertow;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLHandshakeException;

import io.undertow.Undertow.Builder;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import org.apache.jasper.servlet.JspServlet;
import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.boot.testsupport.web.servlet.ExampleServlet;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.MimeMappings.Mapping;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactoryTests;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UndertowServletWebServerFactory}.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 */
public class UndertowServletWebServerFactoryTests
		extends AbstractServletWebServerFactoryTests {

	@Override
	protected UndertowServletWebServerFactory getFactory() {
		return new UndertowServletWebServerFactory(0);
	}

	@Test
	public void errorPage404() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/hello"));
		this.webServer = factory.getWebServer(
				new ServletRegistrationBean<>(new ExampleServlet(), "/hello"));
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
		assertThat(getResponse(getLocalUrl("/not-found"))).isEqualTo("Hello World");
	}

	@Test
	public void setNullBuilderCustomizersThrows() {
		UndertowServletWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.setBuilderCustomizers(null);
	}

	@Test
	public void addNullAddBuilderCustomizersThrows() {
		UndertowServletWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.addBuilderCustomizers((UndertowBuilderCustomizer[]) null);
	}

	@Test
	public void builderCustomizers() throws Exception {
		UndertowServletWebServerFactory factory = getFactory();
		UndertowBuilderCustomizer[] customizers = new UndertowBuilderCustomizer[4];
		for (int i = 0; i < customizers.length; i++) {
			customizers[i] = mock(UndertowBuilderCustomizer.class);
		}
		factory.setBuilderCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addBuilderCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) customizers);
		for (UndertowBuilderCustomizer customizer : customizers) {
			ordered.verify(customizer).customize((Builder) any());
		}
	}

	@Test
	public void setNullDeploymentInfoCustomizersThrows() {
		UndertowServletWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.setDeploymentInfoCustomizers(null);
	}

	@Test
	public void addNullAddDeploymentInfoCustomizersThrows() {
		UndertowServletWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.addDeploymentInfoCustomizers((UndertowDeploymentInfoCustomizer[]) null);
	}

	@Test
	public void deploymentInfo() throws Exception {
		UndertowServletWebServerFactory factory = getFactory();
		UndertowDeploymentInfoCustomizer[] customizers = new UndertowDeploymentInfoCustomizer[4];
		for (int i = 0; i < customizers.length; i++) {
			customizers[i] = mock(UndertowDeploymentInfoCustomizer.class);
		}
		factory.setDeploymentInfoCustomizers(
				Arrays.asList(customizers[0], customizers[1]));
		factory.addDeploymentInfoCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) customizers);
		for (UndertowDeploymentInfoCustomizer customizer : customizers) {
			ordered.verify(customizer).customize((DeploymentInfo) any());
		}
	}

	@Test
	public void basicSslClasspathKeyStore() throws Exception {
		testBasicSslWithKeyStore("classpath:test.jks");
	}

	@Test
	public void defaultContextPath() throws Exception {
		UndertowServletWebServerFactory factory = getFactory();
		final AtomicReference<String> contextPath = new AtomicReference<>();
		factory.addDeploymentInfoCustomizers(
				(deploymentInfo) -> contextPath.set(deploymentInfo.getContextPath()));
		this.webServer = factory.getWebServer();
		assertThat(contextPath.get()).isEqualTo("/");
	}

	@Test
	public void useForwardHeaders() throws Exception {
		UndertowServletWebServerFactory factory = getFactory();
		factory.setUseForwardHeaders(true);
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	public void eachFactoryUsesADiscreteServletContainer() {
		assertThat(getServletContainerFromNewFactory())
				.isNotEqualTo(getServletContainerFromNewFactory());
	}

	@Test
	public void accessLogCanBeEnabled()
			throws IOException, URISyntaxException, InterruptedException {
		testAccessLog(null, null, "access_log.log");
	}

	@Test
	public void accessLogCanBeCustomized()
			throws IOException, URISyntaxException, InterruptedException {
		testAccessLog("my_access.", "logz", "my_access.logz");
	}

	private void testAccessLog(String prefix, String suffix, String expectedFile)
			throws IOException, URISyntaxException, InterruptedException {
		UndertowServletWebServerFactory factory = getFactory();
		factory.setAccessLogEnabled(true);
		factory.setAccessLogPrefix(prefix);
		factory.setAccessLogSuffix(suffix);
		File accessLogDirectory = this.temporaryFolder.getRoot();
		factory.setAccessLogDirectory(accessLogDirectory);
		assertThat(accessLogDirectory.listFiles()).isEmpty();
		this.webServer = factory.getWebServer(
				new ServletRegistrationBean<>(new ExampleServlet(), "/hello"));
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
		File accessLog = new File(accessLogDirectory, expectedFile);
		awaitFile(accessLog);
		assertThat(accessLogDirectory.listFiles()).contains(accessLog);
	}

	@Override
	protected void addConnector(final int port, AbstractServletWebServerFactory factory) {
		((UndertowServletWebServerFactory) factory).addBuilderCustomizers(
				(builder) -> builder.addHttpListener(port, "0.0.0.0"));
	}

	@Test(expected = SSLHandshakeException.class)
	public void sslRestrictedProtocolsEmptyCipherFailure() throws Exception {
		testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.2" },
				new String[] { "TLS_EMPTY_RENEGOTIATION_INFO_SCSV" });
	}

	@Test(expected = SSLHandshakeException.class)
	public void sslRestrictedProtocolsECDHETLS1Failure() throws Exception {
		testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1" },
				new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" });
	}

	@Test
	public void sslRestrictedProtocolsECDHESuccess() throws Exception {
		testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.2" },
				new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" });
	}

	@Test
	public void sslRestrictedProtocolsRSATLS12Success() throws Exception {
		testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.2" },
				new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA256" });
	}

	@Test(expected = SSLHandshakeException.class)
	public void sslRestrictedProtocolsRSATLS11Failure() throws Exception {
		testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.1" },
				new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA256" });
	}

	@Test
	public void getKeyManagersWhenAliasIsNullShouldNotDecorate() throws Exception {
		UndertowServletWebServerFactory factory = getFactory();
		Ssl ssl = getSsl(null, "password", "src/test/resources/test.jks");
		factory.setSsl(ssl);
		KeyManager[] keyManagers = ReflectionTestUtils.invokeMethod(factory,
				"getKeyManagers");
		Class<?> name = Class.forName("org.springframework.boot.web.embedded.undertow"
				+ ".UndertowServletWebServerFactory$ConfigurableAliasKeyManager");
		assertThat(keyManagers[0]).isNotInstanceOf(name);
	}

	@Override
	protected JspServlet getJspServlet() {
		return null; // Undertow does not support JSPs
	}

	private void awaitFile(File file) throws InterruptedException {
		long end = System.currentTimeMillis() + 10000;
		while (!file.exists() && System.currentTimeMillis() < end) {
			Thread.sleep(100);
		}
	}

	private ServletContainer getServletContainerFromNewFactory() {
		UndertowServletWebServer container = (UndertowServletWebServer) getFactory()
				.getWebServer();
		try {
			return ((DeploymentManager) ReflectionTestUtils.getField(container,
					"manager")).getDeployment().getServletContainer();
		}
		finally {
			container.stop();
		}
	}

	@Override
	protected Map<String, String> getActualMimeMappings() {
		return ((DeploymentManager) ReflectionTestUtils.getField(this.webServer,
				"manager")).getDeployment().getMimeExtensionMappings();
	}

	@Override
	protected Collection<Mapping> getExpectedMimeMappings() {
		// Unlike Tomcat and Jetty, Undertow performs a case-sensitive match on file
		// extension so it has a mapping for "z" and "Z".
		Set<Mapping> expectedMappings = new HashSet<>(super.getExpectedMimeMappings());
		expectedMappings.add(new Mapping("Z", "application/x-compress"));
		return expectedMappings;
	}

	@Override
	protected Charset getCharset(Locale locale) {
		DeploymentInfo info = ((DeploymentManager) ReflectionTestUtils
				.getField(this.webServer, "manager")).getDeployment().getDeploymentInfo();
		String charsetName = info.getLocaleCharsetMapping().get(locale.toString());
		return (charsetName != null) ? Charset.forName(charsetName) : null;
	}

	@Override
	protected void handleExceptionCausedByBlockedPort(RuntimeException ex,
			int blockedPort) {
		assertThat(ex).isInstanceOf(PortInUseException.class);
		assertThat(((PortInUseException) ex).getPort()).isEqualTo(blockedPort);
	}

}
